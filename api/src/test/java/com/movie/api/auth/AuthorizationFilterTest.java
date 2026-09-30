package com.movie.api.auth;

import com.movie.api.utils.JwtTokenUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.servlet.FilterChain;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthorizationFilterTest {
    private AuthorizationFilter filter;

    @BeforeEach
    void setUp() {
        new JwtTokenUtil("test-only-signing-key-with-at-least-32-bytes");
        SecurityContextHolder.clearContext();
        filter = new AuthorizationFilter(mock(AuthenticationManager.class));
    }

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void missingHeaderContinuesWithoutAuthentication() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        filter.doFilter(request, response, chain);
        verify(chain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void malformedTokenReturnsTheErrorEnvelopeWithoutCallingController() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "not.a.jwt");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        filter.doFilter(request, response, chain);
        verifyNoInteractions(chain);
        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("\"code\":401"));
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void acceptsLegacyRawAndBearerTokenFormats() throws Exception {
        String token = JwtTokenUtil.createToken("alice", Collections.singletonList("ROLE_USER"), 60000);
        for (String header : new String[] { token, "Bearer " + token }) {
            SecurityContextHolder.clearContext();
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("Authorization", header);
            MockHttpServletResponse response = new MockHttpServletResponse();
            FilterChain chain = mock(FilterChain.class);
            filter.doFilter(request, response, chain);
            verify(chain).doFilter(request, response);
            assertEquals("alice", SecurityContextHolder.getContext().getAuthentication().getName());
        }
    }
}
