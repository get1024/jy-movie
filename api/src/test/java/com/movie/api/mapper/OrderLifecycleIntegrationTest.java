package com.movie.api.mapper;

import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.movie.api.model.entity.Cart;
import com.movie.api.model.entity.Order;
import com.movie.api.service.ArrangementService;
import com.movie.api.service.OrderLifecycleService;
import com.movie.api.service.OrderService;
import com.movie.api.service.impl.ArrangementServiceImpl;
import com.movie.api.service.impl.OrderServiceImpl;
import org.apache.ibatis.session.SqlSessionFactory;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringJUnitConfig(OrderLifecycleIntegrationTest.Config.class)
class OrderLifecycleIntegrationTest {
    @Configuration
    @EnableTransactionManagement
    @MapperScan(basePackageClasses = OrderMapper.class)
    @Import({OrderServiceImpl.class, ArrangementServiceImpl.class, OrderLifecycleService.class})
    static class Config {
        @Bean
        DataSource dataSource() {
            JdbcDataSource source = new JdbcDataSource();
            source.setURL("jdbc:h2:mem:order_lifecycle;MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000");
            return source;
        }
        @Bean
        SqlSessionFactory sqlSessionFactory(DataSource source) throws Exception {
            MybatisSqlSessionFactoryBean factory = new MybatisSqlSessionFactoryBean();
            factory.setDataSource(source);
            return factory.getObject();
        }
        @Bean
        PlatformTransactionManager transactionManager(DataSource source) {
            return new DataSourceTransactionManager(source);
        }
        @Bean
        Clock clock() {
            return Clock.fixed(Instant.parse("2026-09-30T12:00:00Z"), ZoneOffset.UTC);
        }
    }

    @Autowired private DataSource source;
    @Autowired private OrderService orders;
    @Autowired private OrderLifecycleService lifecycle;
    @Autowired private ArrangementService arrangements;
    @Autowired private OrderMapper orderMapper;
    private JdbcTemplate jdbc;

    @BeforeEach
    void schema() {
        jdbc = new JdbcTemplate(source);
        jdbc.execute("DROP ALL OBJECTS");
        jdbc.execute("CREATE TABLE t_order (id VARCHAR(64) PRIMARY KEY, uid VARCHAR(64), phone VARCHAR(32), "
                + "aid VARCHAR(64), seats VARCHAR(255), price DOUBLE, status INT, create_at VARCHAR(32), pay_at VARCHAR(32))");
        jdbc.execute("CREATE TABLE t_arrangement (id VARCHAR(64) PRIMARY KEY, fid VARCHAR(64), name VARCHAR(64), "
                + "seat_number INT, price DOUBLE, type VARCHAR(16), date VARCHAR(32), start_time VARCHAR(32), "
                + "box_office INT, end_time VARCHAR(32), founder VARCHAR(64), create_at VARCHAR(32))");
        jdbc.execute("CREATE TABLE t_film (id VARCHAR(64) PRIMARY KEY, name VARCHAR(64), release_time VARCHAR(32), "
                + "type VARCHAR(32), status BOOLEAN, region VARCHAR(32), introduction VARCHAR(255), hot INT, "
                + "cover VARCHAR(255), duration INT)");
        jdbc.execute("CREATE TABLE t_user (id VARCHAR(64) PRIMARY KEY, username VARCHAR(64), password VARCHAR(255), "
                + "nickname VARCHAR(64), email VARCHAR(128), birthday VARCHAR(32), gender VARCHAR(32), info VARCHAR(255), "
                + "avatar VARCHAR(255), create_at VARCHAR(32), update_at VARCHAR(32))");
        jdbc.execute("CREATE TABLE t_cart (id VARCHAR(64) PRIMARY KEY, uid VARCHAR(64), aid VARCHAR(64), "
                + "seats VARCHAR(255), phone VARCHAR(32), status INT, price DOUBLE)");
        jdbc.update("INSERT INTO t_arrangement (id, fid, seat_number, price) VALUES ('show', 'film', 50, 35.25)");
        jdbc.update("INSERT INTO t_film (id, hot) VALUES ('film', 0)");
        jdbc.update("INSERT INTO t_user (id, username) VALUES ('user', 'viewer')");
    }

    private void insert(String id, int status, String created, String seats) {
        jdbc.update("INSERT INTO t_order (id, uid, aid, status, create_at, seats) VALUES (?, 'user', 'show', ?, ?, ?)",
                id, status, created, seats);
    }

    private Cart cart() {
        Cart cart = new Cart(); cart.setUid("user"); cart.setAid("show");
        cart.setSeats("1号2号"); cart.setStatus(0); cart.setPrice(0.01);
        return cart;
    }

    @Test
    void expirationBoundaryReleasesOnlyExpiredUnpaidSeats() {
        insert("expired", 0, "2026-09-30 11:45:00", "1号");
        insert("waiting", 0, "2026-09-30 11:45:01", "2号");
        insert("paid", 2, "2026-09-29 12:00:00", "3号");
        insert("failed", 1, "2026-09-29 12:00:00", "4号");
        insert("cancelled", 3, "2026-09-29 12:00:00", "5号");
        // Seat release does not depend on waiting for the next scheduler tick.
        assertEquals(Arrays.asList(2, 3), arrangements.getSeatsHaveSelected("show"));
        lifecycle.expireWaitingOrders();
        assertEquals(1, orderMapper.selectById("expired").getStatus());
        assertEquals(0, orderMapper.selectById("waiting").getStatus());
        assertEquals(2, orderMapper.selectById("paid").getStatus());
        assertEquals(3, orderMapper.selectById("cancelled").getStatus());
    }

    @Test
    void timedOutPaymentPersistsFailedStatusAndCannotResurrect() throws Exception {
        insert("expired", 0, "2026-09-30 11:45:00", "1号");
        assertThrows(IllegalArgumentException.class, () -> orders.pay("expired"));
        assertEquals(1, orderMapper.selectById("expired").getStatus());
        assertEquals(Collections.emptyList(), arrangements.getSeatsHaveSelected("show"));
    }

    @Test
    void paidOrderIsIdempotentAndCancellationCannotBeRepaid() throws Exception {
        insert("waiting", 0, "2026-09-30 11:59:00", "1号");
        Order first = orders.pay("waiting");
        assertEquals(2, first.getStatus());
        assertEquals(first.getPayAt(), orders.pay("waiting").getPayAt());
        first.setStatus(3); first.setPrice(0.01); orders.update(first);
        assertThrows(IllegalArgumentException.class, () -> orders.pay("waiting"));
        assertEquals(Collections.emptyList(), arrangements.getSeatsHaveSelected("show"));
    }

    @Test
    void compareAndSetPaymentCannotOverwriteExpiredOrCancelledState() {
        insert("expired", 1, "2026-09-30 11:59:00", "1号");
        insert("cancelled", 3, "2026-09-30 11:59:00", "2号");
        assertEquals(0, orderMapper.payWaitingOrder("expired", lifecycle.now(), lifecycle.expirationCutoff()));
        assertEquals(0, orderMapper.payWaitingOrder("cancelled", lifecycle.now(), lifecycle.expirationCutoff()));
    }

    @Test
    void parallelReservationsCannotSellSameSeatTwice() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CyclicBarrier start = new CyclicBarrier(2);
        Callable<Boolean> reserve = () -> {
            start.await(5, TimeUnit.SECONDS);
            try { orders.create(cart()); return true; }
            catch (IllegalArgumentException conflict) { return false; }
        };
        try {
            Future<Boolean> first = executor.submit(reserve);
            Future<Boolean> second = executor.submit(reserve);
            assertNotEquals(first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS));
            assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM t_order", Integer.class));
            assertEquals(2, jdbc.queryForObject("SELECT hot FROM t_film WHERE id = 'film'", Integer.class));
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void cartConsumptionAndHotUpdateShareOrderTransaction() throws Exception {
        Cart cart = cart(); cart.setId("cart");
        jdbc.update("INSERT INTO t_cart (id, uid, aid, seats) VALUES ('cart', 'user', 'show', '1号2号')");
        Order order = orders.create(cart);
        assertEquals(70.50, order.getPrice());
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM t_cart", Integer.class));
    }

    @Test
    void downstreamFailureRollsBackInsertedOrder() {
        Cart cart = cart(); cart.setId("cart");
        jdbc.update("INSERT INTO t_cart (id, uid, aid, seats) VALUES ('cart', 'user', 'show', '1号2号')");
        jdbc.execute("ALTER TABLE t_film ADD CONSTRAINT hot_cap CHECK (hot < 2)");
        assertThrows(Exception.class, () -> orders.create(cart));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM t_cart", Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM t_order", Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT hot FROM t_film WHERE id = 'film'", Integer.class));
    }
}
