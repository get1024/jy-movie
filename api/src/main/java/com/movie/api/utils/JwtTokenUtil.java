package com.movie.api.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/** JWT compatibility facade for the existing controllers. */
@Component
public final class JwtTokenUtil {

    public static final String TOKEN_HEADER = "Authorization";
    public static final long REMEMBER_EXPIRATION_TIME = 1000L * 60 * 60 * 24 * 7;
    public static final long EXPIRATION_TIME = 1000L * 60 * 60 * 24;
    private static final String ROLE_CLAIMS = "roles";
    private static volatile SecretKey signingKey;

    /** Fail startup rather than silently using a shared development signing key. */
    public JwtTokenUtil(@Value("${jwt.secret}") String secret) {
        if (!StringUtils.hasText(secret) || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("JWT_SECRET must contain at least 32 UTF-8 bytes");
        }
        signingKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    public static String createToken(String username, List<String> roles, long expiration) {
        if (!StringUtils.hasText(username) || expiration <= 0) {
            throw new IllegalArgumentException("Invalid token subject or expiration");
        }
        validateRoles(roles);
        return Jwts.builder()
                .setSubject(username)
                .claim(ROLE_CLAIMS, new ArrayList<>(roles))
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(SignatureAlgorithm.HS256, key())
                .compact();
    }

    /** Verify the signature and expiry once; incomplete claims never authenticate a request. */
    public static Claims parseToken(String token) {
        Claims claims = Jwts.parser().setSigningKey(key()).parseClaimsJws(token).getBody();
        if (!StringUtils.hasText(claims.getSubject()) || claims.getExpiration() == null) {
            throw new JwtException("Token subject and expiration are required");
        }
        getTokenRoles(claims);
        return claims;
    }

    public static String getUsername(String token) {
        return parseToken(token).getSubject();
    }

    public static List<String> getTokenRoles(String token) {
        return getTokenRoles(parseToken(token));
    }

    public static List<String> getTokenRoles(Claims claims) {
        Object roles = claims.get(ROLE_CLAIMS);
        if (!(roles instanceof List<?>)) {
            throw new JwtException("Token roles are required");
        }
        List<String> result = new ArrayList<>();
        for (Object role : (List<?>) roles) {
            if (!(role instanceof String) || !StringUtils.hasText((String) role)) {
                throw new JwtException("Invalid token role");
            }
            result.add((String) role);
        }
        validateRoles(result);
        return result;
    }

    public static boolean isExpiration(String token) {
        try {
            parseToken(token);
            return false;
        } catch (ExpiredJwtException e) {
            return true;
        }
    }

    private static void validateRoles(List<String> roles) {
        if (roles == null || roles.isEmpty()) {
            throw new IllegalArgumentException("At least one token role is required");
        }
        for (String role : roles) {
            if (!StringUtils.hasText(role)) {
                throw new IllegalArgumentException("Invalid token role");
            }
        }
    }

    private static SecretKey key() {
        SecretKey key = signingKey;
        if (key == null) {
            throw new IllegalStateException("JWT signing key has not been configured");
        }
        return key;
    }
}
