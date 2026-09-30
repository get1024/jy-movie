package com.movie.api.service.impl;

import com.movie.api.mapper.ArrangementMapper;
import com.movie.api.mapper.CartMapper;
import com.movie.api.mapper.FilmMapper;
import com.movie.api.model.entity.Arrangement;
import com.movie.api.model.entity.Cart;
import com.movie.api.model.entity.Film;
import com.movie.api.model.vo.CartVO;
import com.movie.api.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceImplTest {
    @Mock private CartMapper carts;
    @Mock private OrderService orders;
    @Mock private ArrangementMapper arrangements;
    @Mock private FilmMapper films;
    private CartServiceImpl service;

    @BeforeEach
    void setUp() { service = new CartServiceImpl(carts, orders, arrangements, films); }

    private Cart cart(String id, String aid) {
        Cart cart = new Cart(); cart.setId(id); cart.setAid(aid); cart.setUid("user");
        cart.setSeats("1号2号"); cart.setPrice(0.01); cart.setStatus(2);
        return cart;
    }

    @Test
    void savingCartDerivesPriceAndResetsClientStatusAndId() {
        Arrangement arrangement = new Arrangement(); arrangement.setPrice(19.99); arrangement.setSeatNumber(20);
        when(arrangements.selectById("a")).thenReturn(arrangement);
        Cart cart = cart("client-id", "a"); service.save(cart);
        assertEquals(39.98, cart.getPrice()); assertEquals(0, cart.getStatus());
        assertNotEquals("client-id", cart.getId()); verify(carts).insert(cart);
    }

    @Test
    void listBatchesRelationsAndKeepsStaleCartRemovable() {
        Cart first = cart("1", "a"); Cart stale = cart("2", "removed");
        Arrangement arrangement = new Arrangement(); arrangement.setId("a"); arrangement.setFid("f");
        Film film = new Film(); film.setId("f");
        when(carts.selectList(any())).thenReturn(Arrays.asList(first, stale));
        when(arrangements.selectBatchIds(anyCollection())).thenReturn(Collections.singletonList(arrangement));
        when(films.selectBatchIds(anyCollection())).thenReturn(Collections.singletonList(film));
        List<CartVO> result = service.findAllByUserId("user");
        assertSame(film, result.get(0).getFilm()); assertNull(result.get(1).getFilm());
        assertSame(stale, result.get(1).getCart());
        verify(arrangements, never()).selectById(any()); verify(films, never()).selectById(any());
    }

    @Test
    void emptyCartSkipsRelatedQueriesAndEmptyDeletes() {
        assertTrue(service.findAllByUserId("u").isEmpty());
        service.deleteCarts(Collections.emptyList());
        verifyNoInteractions(arrangements, films);
        verify(carts, never()).deleteBatchIds(anyCollection());
    }

    @Test
    void settlementUsesConsistentLockOrderWithoutMutatingCallerList() throws Exception {
        Cart second = cart("2", "b"); Cart first = cart("1", "a");
        List<Cart> input = Arrays.asList(second, first); service.settleCarts(input);
        org.mockito.InOrder ordered = inOrder(orders);
        ordered.verify(orders).create(first); ordered.verify(orders).create(second);
        assertSame(second, input.get(0));
    }
}
