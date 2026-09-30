package com.movie.api.service.impl;

import com.movie.api.mapper.LeavingMessageMapper;
import com.movie.api.mapper.UserMapper;
import com.movie.api.model.dto.MessageCount;
import com.movie.api.model.entity.LeavingMessage;
import com.movie.api.model.entity.User;
import com.movie.api.model.vo.ActiveUserVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeavingMessageServiceImplTest {
    @Mock private LeavingMessageMapper leavingMessageMapper;
    @Mock private UserMapper userMapper;
    @InjectMocks private LeavingMessageServiceImpl service;

    @Test
    void activeUsersUseOneAggregateQueryAndKeepZeroCountUsers() {
        User first = new User(); first.setId("u1"); User second = new User(); second.setId("u2");
        MessageCount count = new MessageCount(); count.setUid("u2"); count.setNumber(3);
        when(userMapper.selectList(null)).thenReturn(Arrays.asList(first, second));
        when(leavingMessageMapper.countByUser()).thenReturn(Collections.singletonList(count));
        List<ActiveUserVO> result = service.findActiveUsers();
        assertSame(second, result.get(0).getUser()); assertEquals(3, result.get(0).getNumber());
        assertSame(first, result.get(1).getUser()); assertEquals(0, result.get(1).getNumber());
        verify(leavingMessageMapper, never()).selectList(any());
    }

    @Test
    void messageUserHydrationIsBatched() {
        LeavingMessage first = new LeavingMessage(); first.setUid("u");
        LeavingMessage second = new LeavingMessage(); second.setUid("u");
        when(leavingMessageMapper.selectList(null)).thenReturn(Arrays.asList(first, second));
        assertEquals(2, service.findAll().size());
        verify(userMapper).selectBatchIds(Collections.singletonList("u"));
        verify(userMapper, never()).selectById(any());
    }
}
