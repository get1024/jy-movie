package com.movie.api.model.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PasswordSerializationTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void userWorkerAndAdminPasswordsAreWriteOnly() throws Exception {
        String input = "{\"id\":\"test-id\",\"password\":\"test-password\"}";
        User user = objectMapper.readValue(input, User.class);
        Worker worker = objectMapper.readValue(input, Worker.class);
        Admin admin = objectMapper.readValue(input, Admin.class);
        assertEquals("test-password", user.getPassword());
        assertEquals("test-password", worker.getPassword());
        assertEquals("test-password", admin.getPassword());
        for (Object value : new Object[] { user, worker, admin }) {
            assertFalse(objectMapper.readTree(objectMapper.writeValueAsString(value)).has("password"));
            assertFalse(value.toString().contains("test-password"));
        }
    }
}
