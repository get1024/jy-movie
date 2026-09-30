package com.movie.api.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public final class ResponseUtil {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Logger LOGGER = LoggerFactory.getLogger(ResponseUtil.class);

    private ResponseUtil() {
    }

    public static void writeJson(HttpServletResponse response, Object value) {
        writeJson(response, value, HttpServletResponse.SC_OK);
    }

    public static void writeJson(HttpServletResponse response, Object value, int status) {
        // CORS is handled centrally by Spring Security. Preserve the existing API envelope.
        response.setContentType("application/json;charset=utf-8");
        response.setStatus(status);
        try {
            OBJECT_MAPPER.writeValue(response.getWriter(), value);
        } catch (IOException exception) {
            LOGGER.warn("Unable to write API response", exception);
        }
    }
}
