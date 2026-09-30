package com.movie.api.handler;

import com.movie.api.annotation.DisableBaseResponse;
import com.movie.api.model.support.ResponseResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(Exception.class)
    @DisableBaseResponse
    public ResponseResult<?> handleException(Exception exception) {
        if (exception instanceof AccessDeniedException) {
            return new ResponseResult<>(403, "你没有访问权限");
        }
        if (exception instanceof HttpMessageNotReadableException
                || exception instanceof ServletRequestBindingException
                || exception instanceof MethodArgumentTypeMismatchException) {
            return new ResponseResult<>(400, "请求参数无效");
        }
        // Existing services use checked Exception for user-facing business rejections.
        if (exception instanceof IllegalArgumentException || exception.getClass() == Exception.class) {
            return new ResponseResult<>(400, exception.getMessage());
        }
        LOGGER.error("Unexpected API failure", exception);
        return new ResponseResult<>(500, "服务器暂时无法处理请求，请稍后重试");
    }
}
