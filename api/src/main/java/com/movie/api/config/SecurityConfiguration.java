package com.movie.api.config;

import com.movie.api.auth.AuthorizationFilter;
import com.movie.api.model.support.ResponseResult;
import com.movie.api.utils.ResponseUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * SpringSecurity配置
 */
@Configuration
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfiguration extends WebSecurityConfigurerAdapter {

    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * HTTP验证规则
     *
     * @param http h
     * @throws Exception e
     */
    @Override
    protected void configure(HttpSecurity http) throws Exception {

        //开启跨域
        http.csrf().disable().cors();

        //允许跨域使用iframe
        http.headers().frameOptions().disable();

        //禁用session
        http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);

        //身份验证失败
        http.exceptionHandling().authenticationEntryPoint((request, response, authException) -> {
            ResponseUtil.writeJson(response, new ResponseResult<>(401, "身份认证失败, 请重新登录"), 401);
        }).accessDeniedHandler((request, response, exception) ->
                ResponseUtil.writeJson(response, new ResponseResult<>(403, "你没有访问权限"), 403));

        // The catalog and review pages are public; account and mutation APIs require a token.
        // Existing method-level permissions remain in force. Resource ownership checks are separate.
        http.authorizeRequests()
                .antMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .antMatchers(HttpMethod.POST, "/api/user/login", "/api/user/register",
                        "/api/worker/login", "/api/admin/login").permitAll()
                .antMatchers(HttpMethod.GET, "/api/film", "/api/film/**",
                        "/api/arrangement", "/api/arrangement/**", "/api/poster", "/api/poster/**",
                        "/api/activity", "/api/activity/**", "/api/fe", "/api/lm",
                        "/api/worker", "/api/we", "/api/upload").permitAll()
                .antMatchers("/swagger-ui.html", "/swagger-resources/**", "/v2/api-docs", "/webjars/**",
                        "/error").permitAll()
                .antMatchers("/api/**").authenticated()
                .anyRequest().denyAll();

        http.addFilter(new AuthorizationFilter(authenticationManagerBean()));

    }

    /**
     * SpringSecurity有默认的跨域配置 会无法放行RequestHeader带有"Authorization"请求
     * 防止前端请求api报出cors error
     *
     * @return *
     */
    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        final UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        final CorsConfiguration corsConfiguration = new CorsConfiguration();
        corsConfiguration.addAllowedHeader("*");
        corsConfiguration.addAllowedHeader("DELETE");
        corsConfiguration.addAllowedMethod("*");
        corsConfiguration.addAllowedOrigin("*");
        source.registerCorsConfiguration("/**", corsConfiguration);
        return source;
    }

}
