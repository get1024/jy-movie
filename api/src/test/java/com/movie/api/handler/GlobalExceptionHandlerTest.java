package com.movie.api.handler;

import com.movie.api.model.support.ResponseResult;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.security.access.AccessDeniedException;
import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void preservesBusinessAndAuthorizationErrors() {
        assertEquals("请选择有效的座位", handler.handleException(new IllegalArgumentException("请选择有效的座位")).getMsg());
        assertEquals(403, handler.handleException(new AccessDeniedException("denied")).getCode());
    }

    @Test
    void hidesInternalDatabaseFailureDetails() {
        ResponseResult<?> result = handler.handleException(new DataAccessResourceFailureException("private JDBC connection detail"));
        assertEquals(500, result.getCode());
        assertFalse(result.getMsg().contains("JDBC"));
        assertFalse(result.isSuccess());
    }
}
