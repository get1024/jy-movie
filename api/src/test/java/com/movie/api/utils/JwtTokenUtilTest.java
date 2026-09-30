package com.movie.api.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenUtilTest {
    private static final String TEST_SECRET = "test-only-signing-key-with-at-least-32-bytes";

    @BeforeEach
    void configure() {
        new JwtTokenUtil(TEST_SECRET);
    }

    @Test
    void rejectsMissingOrWeakConfiguration() {
        assertThrows(IllegalArgumentException.class, () -> new JwtTokenUtil(null));
        assertThrows(IllegalArgumentException.class, () -> new JwtTokenUtil("short"));
        assertThrows(IllegalArgumentException.class, () -> new JwtTokenUtil(" ".repeat(32)));
    }

    @Test
    void preservesSubjectRolesAndExpiry() {
        String token = JwtTokenUtil.createToken("alice", Collections.singletonList("ROLE_USER"), 60000);
        Claims claims = JwtTokenUtil.parseToken(token);
        assertEquals("alice", claims.getSubject());
        assertEquals(Collections.singletonList("ROLE_USER"), JwtTokenUtil.getTokenRoles(claims));
        assertTrue(claims.getExpiration().after(new Date()));
        assertFalse(JwtTokenUtil.isExpiration(token));
    }

    @Test
    void rejectsMalformedAndWrongSignatureTokens() {
        assertThrows(JwtException.class, () -> JwtTokenUtil.parseToken("not.a.jwt"));
        String token = JwtTokenUtil.createToken("alice", Collections.singletonList("ROLE_USER"), 60000);
        new JwtTokenUtil("a-different-test-only-key-over-32-bytes");
        assertThrows(JwtException.class, () -> JwtTokenUtil.parseToken(token));
    }

    @Test
    void rejectsExpiredAndIncompleteClaims() {
        SecretKeySpec key = new SecretKeySpec(TEST_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        String expired = Jwts.builder().setSubject("alice").claim("roles", Collections.singletonList("ROLE_USER"))
                .setExpiration(new Date(System.currentTimeMillis() - 10000))
                .signWith(SignatureAlgorithm.HS256, key).compact();
        assertThrows(ExpiredJwtException.class, () -> JwtTokenUtil.parseToken(expired));
        assertTrue(JwtTokenUtil.isExpiration(expired));
        String noSubject = Jwts.builder().claim("roles", Collections.singletonList("ROLE_USER"))
                .setExpiration(new Date(System.currentTimeMillis() + 60000))
                .signWith(SignatureAlgorithm.HS256, key).compact();
        assertThrows(JwtException.class, () -> JwtTokenUtil.parseToken(noSubject));
        String noExpiry = Jwts.builder().setSubject("alice").claim("roles", Collections.singletonList("ROLE_USER"))
                .signWith(SignatureAlgorithm.HS256, key).compact();
        assertThrows(JwtException.class, () -> JwtTokenUtil.parseToken(noExpiry));
        String invalidRoles = Jwts.builder().setSubject("alice").claim("roles", Collections.singletonList(1))
                .setExpiration(new Date(System.currentTimeMillis() + 60000))
                .signWith(SignatureAlgorithm.HS256, key).compact();
        assertThrows(JwtException.class, () -> JwtTokenUtil.parseToken(invalidRoles));
    }
}
