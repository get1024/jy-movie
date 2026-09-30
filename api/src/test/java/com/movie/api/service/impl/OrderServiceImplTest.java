package com.movie.api.service.impl;

import com.movie.api.constant.OrderStatus;
import com.movie.api.mapper.*;
import com.movie.api.model.entity.*;
import com.movie.api.model.vo.OrderVO;
import com.movie.api.service.ArrangementService;
import com.movie.api.service.OrderLifecycleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {
    @Mock private OrderMapper orders;
    @Mock private ArrangementMapper arrangements;
    @Mock private ArrangementService arrangementService;
    @Mock private UserMapper users;
    @Mock private FilmMapper films;
    @Mock private CartMapper carts;
    private OrderServiceImpl service;
    private OrderLifecycleService lifecycle;

    @BeforeEach
    void setUp() {
        lifecycle = new OrderLifecycleService(orders, Clock.fixed(Instant.parse("2026-09-30T12:00:00Z"), ZoneOffset.UTC));
        service = new OrderServiceImpl(orders, arrangementService, arrangements, users, films, lifecycle, carts);
    }

    private Cart cart() {
        Cart cart = new Cart();
        cart.setId("cart"); cart.setUid("user"); cart.setAid("show"); cart.setSeats("1号2号");
        cart.setStatus(OrderStatus.PAYMENT_WAITING); cart.setPrice(0.01);
        return cart;
    }

    private void showing() {
        Arrangement show = new Arrangement(); show.setId("show"); show.setFid("film");
        show.setSeatNumber(50); show.setPrice(30.50);
        when(arrangements.selectByIdForUpdate("show")).thenReturn(show);
        when(users.selectById("user")).thenReturn(new User());
    }

    private Order order(int status) {
        Order order = new Order(); order.setId("order"); order.setStatus(status);
        order.setCreateAt("2026-09-30 11:50:00");
        return order;
    }

    @Test
    void createsWaitingOrderUsingServerPriceAndConsumesMatchingCart() {
        showing(); when(films.selectById("film")).thenReturn(new Film());
        Order order = service.create(cart());
        assertEquals(61.00, order.getPrice());
        assertEquals(OrderStatus.PAYMENT_WAITING, order.getStatus());
        assertNull(order.getPayAt());
        assertEquals("2026-09-30 12:00:00", order.getCreateAt());
        verify(orders).insert(order); verify(films).incrementHot("film", 2); verify(carts).delete(any());
    }

    @Test
    void keepsExistingDemoPaidCheckout() {
        showing(); when(films.selectById("film")).thenReturn(new Film());
        Cart cart = cart(); cart.setStatus(OrderStatus.PAYMENT_SUCCESSFUL);
        assertEquals("2026-09-30 12:00:00", service.create(cart).getPayAt());
    }

    @Test
    void rejectsReservedSeatWithoutCreatingOrder() {
        showing(); when(arrangementService.getSeatsHaveSelected("show")).thenReturn(Collections.singletonList(2));
        assertThrows(IllegalArgumentException.class, () -> service.create(cart()));
        verify(orders, never()).insert(any()); verifyNoInteractions(films, carts);
    }

    @Test
    void rejectsInvalidSeatAndStatus() {
        showing(); Cart cart = cart(); cart.setSeats("51号");
        assertThrows(IllegalArgumentException.class, () -> service.create(cart));
        cart.setSeats("1号"); cart.setStatus(OrderStatus.COUNTERMAND);
        assertThrows(IllegalArgumentException.class, () -> service.create(cart));
        verify(orders, never()).insert(any());
    }

    @Test
    void returnsAlreadyPaidOrderWithoutChangingTimestamp() {
        Order paid = order(2); paid.setPayAt("2026-09-30 11:52:00");
        when(orders.selectById("order")).thenReturn(paid);
        assertSame(paid, service.pay("order"));
        verify(orders, never()).payWaitingOrder(anyString(), anyString(), anyString());
    }

    @Test
    void rejectsExpiredAndCancelledOrders() {
        when(orders.selectById("order")).thenReturn(order(1), order(3));
        assertEquals("订单支付超时", assertThrows(IllegalArgumentException.class, () -> service.pay("order")).getMessage());
        assertThrows(IllegalArgumentException.class, () -> service.pay("order"));
        verify(orders, never()).payWaitingOrder(anyString(), anyString(), anyString());
    }

    @Test
    void successfulPaymentUsesAtomicTransition() {
        Order paid = order(2);
        when(orders.selectById("order")).thenReturn(order(0), paid);
        when(orders.payWaitingOrder("order", "2026-09-30 12:00:00", "2026-09-30 11:45:00")).thenReturn(1);
        assertSame(paid, service.pay("order"));
    }

    @Test
    void concurrentExpiryCannotBeOverwrittenByPayment() {
        when(orders.selectById("order")).thenReturn(order(0), order(1));
        assertThrows(IllegalArgumentException.class, () -> service.pay("order"));
        verify(orders, never()).updateById(any());
    }

    @Test
    void emptyListsSkipAllHydrationQueries() {
        assertTrue(service.findAll().isEmpty());
        verifyNoInteractions(users, arrangements, films);
    }

    @Test
    void batchHydrationHandlesDeletedRelatedRecordsAndKeepsOrder() {
        Order first = order(2); first.setUid("u"); first.setAid("a");
        Order second = order(2); second.setUid("u"); second.setAid("deleted");
        Arrangement arrangement = new Arrangement(); arrangement.setId("a"); arrangement.setFid("f");
        User user = new User(); user.setId("u"); Film film = new Film(); film.setId("f");
        when(orders.selectList(any())).thenReturn(Arrays.asList(first, second));
        when(users.selectBatchIds(anyCollection())).thenReturn(Collections.singletonList(user));
        when(arrangements.selectBatchIds(anyCollection())).thenReturn(Collections.singletonList(arrangement));
        when(films.selectBatchIds(anyCollection())).thenReturn(Collections.singletonList(film));
        List<OrderVO> result = service.findAll();
        assertEquals(2, result.size()); assertSame(first, result.get(0).getOrder());
        assertSame(film, result.get(0).getFilm()); assertNull(result.get(1).getFilm());
        verify(users).selectBatchIds(Collections.singletonList("u"));
        verify(arrangements, never()).selectById(any()); verify(films, never()).selectById(any());
    }
}
