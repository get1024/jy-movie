package com.movie.api.auth;

import com.movie.api.model.support.ResponseResult;
import com.movie.api.utils.JwtTokenUtil;
import com.movie.api.utils.ResponseUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.util.StringUtils;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Accepts existing raw JWT headers as well as standard Bearer headers. */
public class AuthorizationFilter extends BasicAuthenticationFilter {

    public AuthorizationFilter(AuthenticationManager authenticationManager) {
        super(authenticationManager);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        // A stale token must not prevent a user from signing in again.
        String path = request.getRequestURI().substring(request.getContextPath().length());
        if ("POST".equals(request.getMethod()) && ("/api/user/login".equals(path)
                || "/api/worker/login".equals(path) || "/api/admin/login".equals(path)
                || "/api/user/register".equals(path))) {
            chain.doFilter(request, response);
            return;
        }
        String token = request.getHeader(JwtTokenUtil.TOKEN_HEADER);
        if (!StringUtils.hasText(token) || "null".equals(token)) {
            chain.doFilter(request, response);
            return;
        }
        if (token.regionMatches(true, 0, "Bearer ", 0, 7)) {
            token = token.substring(7).trim();
        }
        try {
            Claims claims = JwtTokenUtil.parseToken(token);
            List<SimpleGrantedAuthority> authorities = new ArrayList<>();
            for (String role : JwtTokenUtil.getTokenRoles(claims)) {
                authorities.add(new SimpleGrantedAuthority(role));
            }
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(claims.getSubject(), null, authorities));
        } catch (ExpiredJwtException e) {
            reject(response, "令牌已过期, 请重新登录");
            return;
        } catch (JwtException | IllegalArgumentException e) {
            reject(response, "无效的身份令牌, 请重新登录");
            return;
        }
        chain.doFilter(request, response);
    }

    private void reject(HttpServletResponse response, String message) {
        SecurityContextHolder.clearContext();
        // Keep the application's existing response envelope for all three clients.
        ResponseUtil.writeJson(response, new ResponseResult<>(401, message), HttpServletResponse.SC_UNAUTHORIZED);
    }
}
