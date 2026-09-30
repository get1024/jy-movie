package com.movie.api.config;

import com.movie.api.utils.JwtTokenUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.util.Collections;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

class SecurityConfigurationTest {
    private AnnotationConfigWebApplicationContext context;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext());
        context.register(TestConfiguration.class);
        context.refresh();
        mvc = webAppContextSetup(context).apply(springSecurity()).build();
    }

    @AfterEach
    void cleanup() {
        context.close();
    }

    @Test
    void loginRegistrationAndCatalogRemainPublic() throws Exception {
        for (String path : new String[] { "/api/user/login", "/api/worker/login", "/api/admin/login", "/api/user/register" }) {
            mvc.perform(post(path)).andExpect(status().isOk()).andExpect(content().string("ok"));
            mvc.perform(post(path).header("Authorization", "expired-or-stale-token"))
                    .andExpect(status().isOk()).andExpect(content().string("ok"));
        }
        for (String path : new String[] { "/api/film", "/api/arrangement/getSeats", "/api/poster", "/api/activity", "/api/fe", "/api/lm", "/api/worker", "/api/we", "/api/upload" }) {
            mvc.perform(get(path)).andExpect(status().isOk()).andExpect(content().string("ok"));
        }
    }

    @Test
    void privateReadsAndMutationsRejectAnonymousRequests() throws Exception {
        mvc.perform(get("/api/cart")).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value(401));
        mvc.perform(get("/api/order/pay")).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value(401));
        mvc.perform(post("/api/film")).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value(401));
        mvc.perform(delete("/api/worker")).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void validTokensAccessPrivateApisAndMethodPermissionsStillApply() throws Exception {
        String user = JwtTokenUtil.createToken("alice", Collections.singletonList("ROLE_USER"), 60000);
        mvc.perform(get("/api/cart").header("Authorization", user))
                .andExpect(status().isOk()).andExpect(content().string("ok"));
        mvc.perform(get("/api/role/system").header("Authorization", user)).andExpect(status().isForbidden());
        String admin = JwtTokenUtil.createToken("admin", Collections.singletonList("ROLE_ADMIN"), 60000);
        mvc.perform(get("/api/role/system").header("Authorization", admin))
                .andExpect(status().isOk()).andExpect(content().string("ok"));
    }

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @Import(SecurityConfiguration.class)
    static class TestConfiguration {
        @Bean
        JwtTokenUtil jwtTokenUtil() {
            return new JwtTokenUtil("test-only-signing-key-with-at-least-32-bytes");
        }
        @Bean
        StubController stubController() { return new StubController(); }
    }

    @RestController
    static class StubController {
        @RequestMapping({ "/api/user/login", "/api/worker/login", "/api/admin/login", "/api/user/register",
                "/api/film", "/api/arrangement/getSeats", "/api/poster", "/api/activity", "/api/fe", "/api/lm",
                "/api/worker", "/api/we", "/api/upload", "/api/cart", "/api/order/pay" })
        public String ok() { return "ok"; }

        @PreAuthorize("hasRole('ADMIN')")
        @RequestMapping("/api/role/system")
        public String admin() { return "ok"; }
    }
}
