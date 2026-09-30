package com.movie.api.service;

import com.movie.api.constant.OrderStatus;
import com.movie.api.mapper.OrderMapper;
import com.movie.api.utils.DataTimeUtil;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Clock;

@Service
public class OrderLifecycleService {
    private final OrderMapper orderMapper;
    private final Clock clock;

    public OrderLifecycleService(OrderMapper orderMapper, Clock clock) {
        this.orderMapper = orderMapper;
        this.clock = clock;
    }

    public String now() {
        return DataTimeUtil.formatTimestamp(clock.instant(), clock.getZone());
    }

    public String expirationCutoff() {
        return DataTimeUtil.formatTimestamp(clock.instant().minusMillis(OrderStatus.EXPIRATION_TIME), clock.getZone());
    }

    @Scheduled(fixedDelayString = "${movie.order.expiration-check-delay-ms:60000}")
    public void expireWaitingOrders() {
        // Compare-and-set: the scheduler must never overwrite a concurrent successful payment.
        orderMapper.expireWaitingOrders(expirationCutoff());
    }
}
