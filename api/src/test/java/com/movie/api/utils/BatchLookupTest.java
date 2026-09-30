package com.movie.api.utils;

import com.movie.api.mapper.UserMapper;
import com.movie.api.model.entity.User;
import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class BatchLookupTest {
    @Test
    void skipsEmptyBatchesAndDeduplicatesIds() {
        UserMapper mapper = mock(UserMapper.class);
        assertTrue(BatchLookup.byId(Collections.<String>emptyList(), mapper, User::getId).isEmpty());
        verifyNoInteractions(mapper);
        User user = new User(); user.setId("u");
        when(mapper.selectBatchIds(Collections.singletonList("u"))).thenReturn(Collections.singletonList(user));
        assertSame(user, BatchLookup.byId(Arrays.asList(null, "u", "u"), mapper, User::getId).get("u"));
        verify(mapper).selectBatchIds(Collections.singletonList("u"));
    }

    @Test
    void limitsEachInQueryToFiveHundredIds() {
        UserMapper mapper = mock(UserMapper.class);
        List<String> ids = IntStream.range(0, 1001).mapToObj(Integer::toString).collect(Collectors.toList());
        BatchLookup.byId(ids, mapper, User::getId);
        verify(mapper, times(3)).selectBatchIds(argThat(batch -> batch.size() <= 500));
    }
}
