package com.movie.api.service.impl;

import com.movie.api.mapper.AdminMapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.movie.api.mapper.UserMapper;
import com.movie.api.mapper.WorkerMapper;
import com.movie.api.model.dto.LoginDto;
import com.movie.api.model.entity.Admin;
import com.movie.api.model.entity.User;
import com.movie.api.model.entity.Worker;
import com.movie.api.utils.JwtTokenUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AccountServiceTest {
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);

    @BeforeEach
    void configureJwt() {
        new JwtTokenUtil("test-only-signing-key-with-at-least-32-bytes");
    }

    @Test
    void userProfileAndAvatarChangesPreserveAnOmittedPassword() {
        UserMapper mapper = mock(UserMapper.class);
        UserServiceImpl service = new UserServiceImpl();
        ReflectionTestUtils.setField(service, "userMapper", mapper);
        ReflectionTestUtils.setField(service, "bCryptPasswordEncoder", encoder);
        User existing = new User();
        existing.setId("user-1");
        existing.setPassword(encoder.encode("original-test-password"));
        when(mapper.selectById("user-1")).thenReturn(existing);
        User update = new User();
        update.setId("user-1");
        update.setNickname("New nickname");
        update.setAvatar("/api/upload?id=test");
        service.update(update);
        verify(mapper).updateById(update);
        assertNull(update.getPassword());
        assertNotNull(update.getUpdateAt());
    }

    @Test
    void workerProfileChangesPreserveBlankPasswordAndHashNewPasswords() throws Exception {
        WorkerMapper mapper = mock(WorkerMapper.class);
        WorkerServiceImpl service = new WorkerServiceImpl();
        ReflectionTestUtils.setField(service, "workerMapper", mapper);
        ReflectionTestUtils.setField(service, "bCryptPasswordEncoder", encoder);
        Worker existing = new Worker();
        existing.setId("worker-1");
        existing.setPassword(encoder.encode("original-test-password"));
        when(mapper.selectById("worker-1")).thenReturn(existing);
        Worker update = new Worker();
        update.setId("worker-1");
        update.setUsername("worker");
        update.setPassword("");
        service.update(update);
        assertNull(update.getPassword());
        update.setPassword("new-test-password");
        service.update(update);
        assertTrue(encoder.matches("new-test-password", update.getPassword()));
        verify(mapper, times(2)).updateById(update);
    }

    @Test
    void legacyAdminPasswordsUpgradeAfterSuccessfulLoginOnly() throws Exception {
        AdminMapper mapper = mock(AdminMapper.class);
        AdminServiceImpl service = adminService(mapper);
        Admin admin = new Admin("admin-1", "admin", "legacy-test-password", null);
        when(mapper.selectOne(any())).thenReturn(admin);
        when(mapper.update(isNull(), any())).thenReturn(1);
        LoginDto dto = login("admin", "legacy-test-password");
        String token = service.login(dto);
        assertEquals("admin", JwtTokenUtil.getUsername(token));
        ArgumentCaptor<UpdateWrapper<Admin>> update = ArgumentCaptor.forClass(UpdateWrapper.class);
        verify(mapper).update(isNull(), update.capture());
        String condition = update.getValue().getSqlSegment();
        assertTrue(condition.contains("id"));
        assertTrue(condition.contains("password"));
        assertTrue(update.getValue().getParamNameValuePairs().containsValue("admin-1"));
        assertTrue(update.getValue().getParamNameValuePairs().containsValue("legacy-test-password"));
        assertTrue(update.getValue().getParamNameValuePairs().values().stream()
                .filter(value -> value instanceof String && ((String) value).startsWith("$2"))
                .anyMatch(value -> encoder.matches("legacy-test-password", (String) value)));
    }

    @Test
    void wrongLegacyAdminPasswordNeverChangesTheAccount() {
        AdminMapper mapper = mock(AdminMapper.class);
        AdminServiceImpl service = adminService(mapper);
        when(mapper.selectOne(any())).thenReturn(new Admin("admin-1", "admin", "legacy-test-password", null));
        assertThrows(Exception.class, () -> service.login(login("admin", "wrong-password")));
        verify(mapper, never()).update(isNull(), any());
    }

    @Test
    void encodedAdminPasswordAuthenticatesWithoutRehashing() throws Exception {
        AdminMapper mapper = mock(AdminMapper.class);
        AdminServiceImpl service = adminService(mapper);
        when(mapper.selectOne(any())).thenReturn(new Admin("admin-1", "admin", encoder.encode("test-password"), null));
        assertNotNull(service.login(login("admin", "test-password")));
        verify(mapper, never()).update(isNull(), any());
    }

    private AdminServiceImpl adminService(AdminMapper mapper) {
        AdminServiceImpl service = new AdminServiceImpl();
        ReflectionTestUtils.setField(service, "adminMapper", mapper);
        ReflectionTestUtils.setField(service, "bCryptPasswordEncoder", encoder);
        return service;
    }

    private LoginDto login(String username, String password) {
        LoginDto dto = new LoginDto();
        dto.setUsername(username);
        dto.setPassword(password);
        return dto;
    }
}
