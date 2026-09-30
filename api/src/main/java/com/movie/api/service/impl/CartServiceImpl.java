package com.movie.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.movie.api.constant.OrderStatus;
import com.movie.api.mapper.ArrangementMapper;
import com.movie.api.mapper.CartMapper;
import com.movie.api.mapper.FilmMapper;
import com.movie.api.model.entity.Arrangement;
import com.movie.api.model.entity.Cart;
import com.movie.api.model.entity.Film;
import com.movie.api.model.vo.CartVO;
import com.movie.api.service.CartService;
import com.movie.api.service.OrderService;
import com.movie.api.utils.BatchLookup;
import com.movie.api.utils.SeatSelection;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CartServiceImpl implements CartService {
    private final CartMapper cartMapper;
    private final OrderService orderService;
    private final ArrangementMapper arrangementMapper;
    private final FilmMapper filmMapper;

    public CartServiceImpl(CartMapper cartMapper, OrderService orderService,
                           ArrangementMapper arrangementMapper, FilmMapper filmMapper) {
        this.cartMapper = cartMapper;
        this.orderService = orderService;
        this.arrangementMapper = arrangementMapper;
        this.filmMapper = filmMapper;
    }

    @Override
    public void save(Cart cart) {
        if (cart == null || cart.getUid() == null || cart.getAid() == null) {
            throw new IllegalArgumentException("请选择用户和场次");
        }
        Arrangement arrangement = arrangementMapper.selectById(cart.getAid());
        if (arrangement == null) throw new IllegalArgumentException("场次不存在");
        int count = SeatSelection.validate(cart.getSeats(), arrangement.getSeatNumber()).size();
        if (!Double.isFinite(arrangement.getPrice()) || arrangement.getPrice() < 0) {
            throw new IllegalArgumentException("场次价格无效");
        }
        cart.setId(UUID.randomUUID().toString());
        cart.setStatus(OrderStatus.PAYMENT_WAITING);
        cart.setPrice(BigDecimal.valueOf(arrangement.getPrice()).multiply(BigDecimal.valueOf(count))
                .setScale(2, RoundingMode.HALF_UP).doubleValue());
        cartMapper.insert(cart);
    }

    @Override
    public void deleteById(String id) {
        cartMapper.deleteById(id);
    }

    @Override
    public void deleteAllByUserId(String uid) {
        cartMapper.delete(new QueryWrapper<Cart>().eq("uid", uid));
    }

    @Override
    public List<CartVO> findAllByUserId(String uid) {
        List<Cart> carts = cartMapper.selectList(new QueryWrapper<Cart>().eq("uid", uid));
        Map<String, Arrangement> arrangements = BatchLookup.byId(
                carts.stream().map(Cart::getAid).collect(Collectors.toList()), arrangementMapper, Arrangement::getId);
        Map<String, Film> films = BatchLookup.byId(
                arrangements.values().stream().map(Arrangement::getFid).collect(Collectors.toList()), filmMapper, Film::getId);
        List<CartVO> result = new ArrayList<>(carts.size());
        for (Cart cart : carts) {
            Arrangement arrangement = arrangements.get(cart.getAid());
            result.add(new CartVO(arrangement == null ? null : films.get(arrangement.getFid()), arrangement, cart));
        }
        return result;
    }

    @Override
    public void deleteCarts(List<Cart> carts) {
        if (carts == null || carts.isEmpty()) return;
        List<String> ids = carts.stream().map(Cart::getId).distinct().collect(Collectors.toList());
        cartMapper.deleteBatchIds(ids);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public void settleCarts(List<Cart> carts) throws Exception {
        if (carts == null || carts.isEmpty()) throw new IllegalArgumentException("请选择购物车条目");
        // Stable showing order reduces lock-order inversions across multi-showing checkouts.
        List<Cart> ordered = new ArrayList<>(carts);
        if (ordered.stream().anyMatch(cart -> cart == null || cart.getAid() == null)) {
            throw new IllegalArgumentException("场次不能为空");
        }
        ordered.sort(Comparator.comparing(Cart::getAid));
        for (Cart cart : ordered) orderService.create(cart);
    }
}
