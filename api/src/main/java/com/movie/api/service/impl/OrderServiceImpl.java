package com.movie.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.movie.api.constant.OrderStatus;
import com.movie.api.mapper.ArrangementMapper;
import com.movie.api.mapper.CartMapper;
import com.movie.api.mapper.FilmMapper;
import com.movie.api.mapper.OrderMapper;
import com.movie.api.mapper.UserMapper;
import com.movie.api.model.entity.Arrangement;
import com.movie.api.model.entity.Cart;
import com.movie.api.model.entity.Film;
import com.movie.api.model.entity.Order;
import com.movie.api.model.entity.User;
import com.movie.api.model.vo.OrderVO;
import com.movie.api.service.ArrangementService;
import com.movie.api.service.OrderLifecycleService;
import com.movie.api.service.OrderService;
import com.movie.api.utils.BatchLookup;
import com.movie.api.utils.SeatSelection;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl implements OrderService {
    private final OrderMapper orderMapper;
    private final CartMapper cartMapper;
    private final ArrangementService arrangementService;
    private final ArrangementMapper arrangementMapper;
    private final UserMapper userMapper;
    private final FilmMapper filmMapper;
    private final OrderLifecycleService lifecycle;

    public OrderServiceImpl(OrderMapper orderMapper, ArrangementService arrangementService,
                            ArrangementMapper arrangementMapper, UserMapper userMapper,
                            FilmMapper filmMapper, OrderLifecycleService lifecycle, CartMapper cartMapper) {
        this.orderMapper = orderMapper;
        this.cartMapper = cartMapper;
        this.arrangementService = arrangementService;
        this.arrangementMapper = arrangementMapper;
        this.userMapper = userMapper;
        this.filmMapper = filmMapper;
        this.lifecycle = lifecycle;
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public Order create(Cart cart) {
        if (cart == null || cart.getAid() == null || cart.getUid() == null) {
            throw new IllegalArgumentException("请选择场次和用户");
        }
        // All reservations for one showing serialize on its existing row. No schema migration needed.
        Arrangement arrangement = arrangementMapper.selectByIdForUpdate(cart.getAid());
        if (arrangement == null) throw new IllegalArgumentException("场次不存在");
        if (userMapper.selectById(cart.getUid()) == null) throw new IllegalArgumentException("用户不存在");
        List<Integer> selected = SeatSelection.validate(cart.getSeats(), arrangement.getSeatNumber());
        Set<Integer> reserved = new HashSet<>(arrangementService.getSeatsHaveSelected(cart.getAid()));
        if (selected.stream().anyMatch(reserved::contains)) {
            throw new IllegalArgumentException("座位已被其他用户预订，请重新选择");
        }
        int status = cart.getStatus() == null ? OrderStatus.PAYMENT_WAITING : cart.getStatus();
        // Status 2 is the existing demonstration checkout, not a real payment-provider confirmation.
        if (status != OrderStatus.PAYMENT_WAITING && status != OrderStatus.PAYMENT_SUCCESSFUL) {
            throw new IllegalArgumentException("订单状态无效");
        }
        if (!Double.isFinite(arrangement.getPrice()) || arrangement.getPrice() < 0) {
            throw new IllegalArgumentException("场次价格无效");
        }
        if (filmMapper.selectById(arrangement.getFid()) == null) {
            throw new IllegalArgumentException("影片不存在");
        }
        String now = lifecycle.now();
        Order order = new Order();
        order.setId(UUID.randomUUID().toString());
        order.setUid(cart.getUid());
        order.setPhone(cart.getPhone());
        order.setAid(cart.getAid());
        order.setStatus(status);
        order.setSeats(cart.getSeats());
        if (status == OrderStatus.PAYMENT_SUCCESSFUL) order.setPayAt(now);
        order.setPrice(BigDecimal.valueOf(arrangement.getPrice()).multiply(BigDecimal.valueOf(selected.size()))
                .setScale(2, RoundingMode.HALF_UP).doubleValue());
        order.setCreateAt(now);
        orderMapper.insert(order);
        filmMapper.incrementHot(arrangement.getFid(), selected.size());
        if (cart.getId() != null) {
            cartMapper.delete(new QueryWrapper<Cart>().eq("id", cart.getId()).eq("uid", cart.getUid())
                    .eq("aid", cart.getAid()).eq("seats", cart.getSeats()));
        }
        return order;
    }

    @Override
    public Order pay(String id) {
        if (id == null || id.trim().isEmpty()) throw new IllegalArgumentException("不存在的订单id");
        lifecycle.expireWaitingOrders();
        Order order = requireOrder(id);
        if (OrderStatus.PAYMENT_SUCCESSFUL.equals(order.getStatus())) return order;
        if (!OrderStatus.PAYMENT_WAITING.equals(order.getStatus())) throw paymentError(order);

        // Atomic transition protects against expiry, cancellation, and duplicate payment requests.
        if (orderMapper.payWaitingOrder(id, lifecycle.now(), lifecycle.expirationCutoff()) == 0) {
            lifecycle.expireWaitingOrders();
            order = requireOrder(id);
            if (OrderStatus.PAYMENT_SUCCESSFUL.equals(order.getStatus())) return order;
            throw paymentError(order);
        }
        return requireOrder(id);
    }

    private Order requireOrder(String id) {
        Order order = orderMapper.selectById(id);
        if (order == null) throw new IllegalArgumentException("不存在的订单id");
        return order;
    }

    private IllegalArgumentException paymentError(Order order) {
        return new IllegalArgumentException(OrderStatus.PAYMENT_FAILED.equals(order.getStatus())
                ? "订单支付超时" : "当前订单状态无法支付");
    }

    @Override
    public void update(Order order) {
        if (order == null || order.getId() == null || !OrderStatus.COUNTERMAND.equals(order.getStatus())) {
            throw new IllegalArgumentException("仅支持撤销订单");
        }
        requireOrder(order.getId());
        // Never let an update replace owner, seats, timestamps, or server-calculated price.
        orderMapper.cancelOrder(order.getId());
    }

    @Override
    public List<OrderVO> findAll() {
        return findByWrapper(new QueryWrapper<>());
    }

    @Override
    public List<OrderVO> findByUser(String uid) {
        return findByWrapper(new QueryWrapper<Order>().eq("uid", uid));
    }

    private List<OrderVO> findByWrapper(QueryWrapper<Order> wrapper) {
        lifecycle.expireWaitingOrders();
        List<Order> orders = orderMapper.selectList(wrapper);
        Map<String, User> users = BatchLookup.byId(orders.stream().map(Order::getUid).collect(Collectors.toList()),
                userMapper, User::getId);
        Map<String, Arrangement> arrangements = BatchLookup.byId(
                orders.stream().map(Order::getAid).collect(Collectors.toList()), arrangementMapper, Arrangement::getId);
        Map<String, Film> films = BatchLookup.byId(
                arrangements.values().stream().map(Arrangement::getFid).collect(Collectors.toList()), filmMapper, Film::getId);
        List<OrderVO> result = new ArrayList<>(orders.size());
        for (Order order : orders) {
            Arrangement arrangement = arrangements.get(order.getAid());
            OrderVO view = new OrderVO();
            view.setOrder(order);
            view.setUser(users.get(order.getUid()));
            view.setArrangement(arrangement);
            view.setFilm(arrangement == null ? null : films.get(arrangement.getFid()));
            result.add(view);
        }
        return result;
    }
}
