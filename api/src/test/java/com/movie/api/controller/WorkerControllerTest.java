package com.movie.api.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.movie.api.model.entity.Worker;
import com.movie.api.service.WorkerService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WorkerControllerTest {
    @AfterEach
    void cleanup() { SecurityContextHolder.clearContext(); }

    @Test
    void publicCardsExcludePrivateEmployeeFieldsButStaffListsKeepThem() throws Exception {
        Worker worker = new Worker();
        worker.setId("worker-1");
        worker.setNickname("Support");
        worker.setPhone("test-private-phone");
        worker.setPassword("test-private-hash");
        WorkerService service = mock(WorkerService.class);
        when(service.findAll()).thenReturn(Collections.singletonList(worker));
        WorkerController controller = new WorkerController();
        ReflectionTestUtils.setField(controller, "workerService", service);
        ObjectMapper mapper = new ObjectMapper();
        SecurityContextHolder.clearContext();
        JsonNode publicCard = mapper.valueToTree(controller.list()).get(0);
        assertEquals("worker-1", publicCard.get("id").asText());
        assertEquals("Support", publicCard.get("nickname").asText());
        assertFalse(publicCard.has("phone"));
        assertFalse(publicCard.has("password"));
        assertFalse(publicCard.has("username"));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "admin", null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))));
        JsonNode staffCard = mapper.valueToTree(controller.list()).get(0);
        assertEquals("test-private-phone", staffCard.get("phone").asText());
        assertFalse(staffCard.has("password"));
    }
}
