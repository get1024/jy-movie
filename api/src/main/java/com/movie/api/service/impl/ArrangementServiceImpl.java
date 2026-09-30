package com.movie.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.movie.api.constant.OrderStatus;
import com.movie.api.mapper.ArrangementMapper;
import com.movie.api.mapper.FilmMapper;
import com.movie.api.mapper.OrderMapper;
import com.movie.api.model.entity.Arrangement;
import com.movie.api.model.entity.Order;
import com.movie.api.model.vo.ArrangementVO;
import com.movie.api.service.ArrangementService;
import com.movie.api.service.OrderLifecycleService;
import com.movie.api.utils.DataTimeUtil;
import com.movie.api.utils.SeatSelection;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Set;

@Service
public class ArrangementServiceImpl implements ArrangementService {

    @Resource
    private ArrangementMapper arrangementMapper;

    @Resource
    private FilmMapper filmMapper;

    @Resource
    private OrderMapper orderMapper;

    @Resource
    private OrderLifecycleService lifecycle;

    @Override
    public void save(Arrangement arrangement) {
        arrangement.setBoxOffice(0);
        arrangement.setCreateAt(DataTimeUtil.getNowTimeString());
        arrangementMapper.insert(arrangement);
    }

    @Override
    public List<Arrangement> findAll() {
        return arrangementMapper.selectList(null);
    }

    @Override
    public ArrangementVO findByFilmId(String fid) {
        List<Arrangement> list = arrangementMapper.selectList(new QueryWrapper<Arrangement>().in("fid", fid));
        return new ArrangementVO(list, filmMapper.selectById(fid));
    }

    @Override
    public List<Integer> getSeatsHaveSelected(String id) {
        List<Order> orders = orderMapper.selectList(new QueryWrapper<Order>().eq("aid", id)
                .and(active -> active.eq("status", OrderStatus.PAYMENT_SUCCESSFUL)
                        .or(waiting -> waiting.eq("status", OrderStatus.PAYMENT_WAITING)
                                .gt("create_at", lifecycle.expirationCutoff()))));
        Set<Integer> seats = new LinkedHashSet<>();
        for (Order order : orders) {
            seats.addAll(SeatSelection.parse(order.getSeats()));
        }
        return new ArrayList<>(seats);
    }

    @Override
    public Arrangement findById(String id) {
        return arrangementMapper.selectById(id);
    }

    @Override
    public void deleteById(String id) {
        arrangementMapper.deleteById(id);
    }

    @Override
    public Arrangement Update(Arrangement arrangement) {
        arrangementMapper.updateById(arrangement);
        return arrangement;
    }

}
