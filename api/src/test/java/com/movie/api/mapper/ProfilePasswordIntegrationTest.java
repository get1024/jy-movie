package com.movie.api.mapper;

import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.movie.api.model.entity.User;
import com.movie.api.model.entity.Admin;
import com.movie.api.model.dto.LoginDto;
import com.movie.api.service.impl.AdminServiceImpl;
import com.movie.api.utils.JwtTokenUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.movie.api.model.entity.Worker;
import com.movie.api.service.impl.UserServiceImpl;
import com.movie.api.service.impl.WorkerServiceImpl;
import org.apache.ibatis.session.SqlSessionFactory;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.util.ReflectionTestUtils;

import javax.sql.DataSource;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Exercises real MyBatis SQL after a password reset interleaves with a profile edit. */
@SpringJUnitConfig(ProfilePasswordIntegrationTest.Config.class)
class ProfilePasswordIntegrationTest {
    @Configuration
    @MapperScan(basePackageClasses = UserMapper.class)
    static class Config {
        @Bean
        DataSource dataSource() {
            JdbcDataSource source = new JdbcDataSource();
            source.setURL("jdbc:h2:mem:profile_password;MODE=MySQL;DB_CLOSE_DELAY=-1");
            return source;
        }

        @Bean
        SqlSessionFactory sqlSessionFactory(DataSource source) throws Exception {
            MybatisSqlSessionFactoryBean factory = new MybatisSqlSessionFactoryBean();
            factory.setDataSource(source);
            return factory.getObject();
        }
    }

    @Autowired private DataSource source;
    @Autowired private UserMapper userMapper;
    @Autowired private AdminMapper adminMapper;
    @Autowired private WorkerMapper workerMapper;
    private JdbcTemplate jdbc;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private String originalHash;
    private String resetHash;

    @BeforeEach
    void schema() {
        jdbc = new JdbcTemplate(source);
        jdbc.execute("DROP ALL OBJECTS");
        jdbc.execute("CREATE TABLE t_user (id VARCHAR(64) PRIMARY KEY, username VARCHAR(64), password VARCHAR(255), "
                + "nickname VARCHAR(64), email VARCHAR(128), birthday VARCHAR(32), gender VARCHAR(32), info VARCHAR(255), "
                + "avatar VARCHAR(255), create_at VARCHAR(32), update_at VARCHAR(32))");
        jdbc.execute("CREATE TABLE t_worker (id VARCHAR(64) PRIMARY KEY, username VARCHAR(64), password VARCHAR(255), "
                + "nickname VARCHAR(64), gender VARCHAR(32), phone VARCHAR(32), avatar VARCHAR(255), entry BOOLEAN, "
                + "department VARCHAR(64), create_at VARCHAR(32), update_at VARCHAR(32))");
        jdbc.execute("CREATE TABLE t_admin (id VARCHAR(64) PRIMARY KEY, username VARCHAR(64), password VARCHAR(255), avatar VARCHAR(255))");
        jdbc.update("INSERT INTO t_admin (id, username, password) VALUES ('admin-1', 'admin', 'legacy-test-password')");
        new JwtTokenUtil("test-only-signing-key-with-at-least-32-bytes");
        originalHash = encoder.encode("original-test-password");
        resetHash = encoder.encode("reset-test-password");
        jdbc.update("INSERT INTO t_user (id, username, password) VALUES ('user-1', 'user', ?)", originalHash);
        jdbc.update("INSERT INTO t_worker (id, username, password) VALUES ('worker-1', 'worker', ?)", originalHash);
    }

    @Test
    void omittedBlankAndUnchangedUserPasswordNeverUndoAConcurrentReset() {
        for (String submitted : new String[] { null, "", originalHash }) {
            jdbc.update("UPDATE t_user SET password=? WHERE id='user-1'", originalHash);
            UserMapper interleavingMapper = mock(UserMapper.class);
            AtomicBoolean resetDone = new AtomicBoolean();
            when(interleavingMapper.selectById("user-1")).thenAnswer(invocation -> {
                User snapshot = userMapper.selectById("user-1");
                if (resetDone.compareAndSet(false, true)) {
                    jdbc.update("UPDATE t_user SET password=? WHERE id='user-1'", resetHash);
                }
                return snapshot;
            });
            when(interleavingMapper.updateById(any(User.class)))
                    .thenAnswer(invocation -> userMapper.updateById(invocation.getArgument(0)));
            UserServiceImpl service = new UserServiceImpl();
            ReflectionTestUtils.setField(service, "userMapper", interleavingMapper);
            ReflectionTestUtils.setField(service, "bCryptPasswordEncoder", encoder);
            User edit = new User();
            edit.setId("user-1"); edit.setNickname("Updated profile");
            edit.setAvatar("/api/upload?id=new-avatar"); edit.setPassword(submitted);
            User returned = service.update(edit);
            assertEquals(resetHash, userMapper.selectById("user-1").getPassword());
            assertEquals("Updated profile", returned.getNickname());
            assertEquals("/api/upload?id=new-avatar", returned.getAvatar());
            assertEquals(resetHash, returned.getPassword());
        }
    }

    @Test
    void omittedBlankAndUnchangedWorkerPasswordNeverUndoAConcurrentReset() throws Exception {
        for (String submitted : new String[] { null, "", originalHash }) {
            jdbc.update("UPDATE t_worker SET password=? WHERE id='worker-1'", originalHash);
            WorkerMapper interleavingMapper = mock(WorkerMapper.class);
            when(interleavingMapper.selectById("worker-1")).thenAnswer(invocation -> {
                Worker snapshot = workerMapper.selectById("worker-1");
                jdbc.update("UPDATE t_worker SET password=? WHERE id='worker-1'", resetHash);
                return snapshot;
            });
            when(interleavingMapper.updateById(any(Worker.class)))
                    .thenAnswer(invocation -> workerMapper.updateById(invocation.getArgument(0)));
            WorkerServiceImpl service = new WorkerServiceImpl();
            ReflectionTestUtils.setField(service, "workerMapper", interleavingMapper);
            ReflectionTestUtils.setField(service, "bCryptPasswordEncoder", encoder);
            Worker edit = new Worker();
            edit.setId("worker-1"); edit.setNickname("Updated profile");
            edit.setAvatar("/api/upload?id=new-avatar"); edit.setPassword(submitted);
            service.update(edit);
            Worker actual = workerMapper.selectById("worker-1");
            assertEquals(resetHash, actual.getPassword());
            assertEquals("Updated profile", actual.getNickname());
            assertEquals("/api/upload?id=new-avatar", actual.getAvatar());
        }
    }

    @Test
    void legacyAdminMigrationCannotOverwriteAConcurrentPasswordReset() {
        AdminMapper interleavingMapper = mock(AdminMapper.class);
        when(interleavingMapper.selectOne(any())).thenAnswer(invocation -> {
            Admin snapshot = adminMapper.selectOne(new QueryWrapper<Admin>().eq("username", "admin"));
            jdbc.update("UPDATE t_admin SET password=? WHERE id='admin-1'", resetHash);
            return snapshot;
        });
        when(interleavingMapper.update(isNull(), any()))
                .thenAnswer(invocation -> adminMapper.update(null, invocation.getArgument(1)));
        AdminServiceImpl service = new AdminServiceImpl();
        ReflectionTestUtils.setField(service, "adminMapper", interleavingMapper);
        ReflectionTestUtils.setField(service, "bCryptPasswordEncoder", encoder);
        LoginDto login = new LoginDto();
        login.setUsername("admin"); login.setPassword("legacy-test-password");
        assertThrows(Exception.class, () -> service.login(login));
        assertEquals(resetHash, adminMapper.selectById("admin-1").getPassword());
    }

    @Test
    void explicitNewPasswordsStillPersistAsHashes() {
        UserServiceImpl service = new UserServiceImpl();
        ReflectionTestUtils.setField(service, "userMapper", userMapper);
        ReflectionTestUtils.setField(service, "bCryptPasswordEncoder", encoder);
        User edit = new User(); edit.setId("user-1"); edit.setPassword("new-test-password");
        User updated = service.update(edit);
        assertTrue(encoder.matches("new-test-password", updated.getPassword()));
        assertEquals(updated.getPassword(), userMapper.selectById("user-1").getPassword());
    }
}
