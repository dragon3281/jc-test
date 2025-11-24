package com.detection.platform.config;

import com.detection.platform.common.utils.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Spring Security配置
 *
 * @author Detection Platform
 * @since 2024-11-12
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtUtil jwtUtil;

    /**
     * 密码编码器
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 安全过滤器链
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 禁用CSRF
                .csrf(AbstractHttpConfigurer::disable)
                
                // 禁用CORS
                .cors(AbstractHttpConfigurer::disable)
                
                // 禁用HTTP Basic认证
                .httpBasic(AbstractHttpConfigurer::disable)
                
                // 禁用表单登录
                .formLogin(AbstractHttpConfigurer::disable)
                
                // 配置会话管理为无状态
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                
                // 配置访问权限
                .authorizeHttpRequests(auth -> auth
                        // 允许登录接口无需认证
                        .requestMatchers("/api/user/login", "/api/user/test-password", "/user/login", "/user/test-password").permitAll()
                        // WebSocket端点允许访问
                        .requestMatchers("/ws/**", "/terminal/**").permitAll()
                        // 业务接口临时开放用于E2E
                        .requestMatchers("/business/**").permitAll()
                        // 其他所有请求都需要认证
                        .anyRequest().authenticated())
                
                // 添加JWT过滤器，但JWT过滤器不应该拦截公开路径
                .addFilterBefore(new JwtAuthenticationFilter(jwtUtil), 
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * JWT认证过滤器
     */
    @Slf4j
    private static class JwtAuthenticationFilter extends OncePerRequestFilter {

        private final JwtUtil jwtUtil;

        public JwtAuthenticationFilter(JwtUtil jwtUtil) {
            this.jwtUtil = jwtUtil;
        }

        @Override
        protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
            String uri = request.getRequestURI();
            // 公开路径完全跳过JWT过滤器
            boolean isPublic = isPublicPath(uri);
            if (isPublic) {
                log.debug("公开路径，跳过JWT过滤器: {}", uri);
            }
            return isPublic;
        }

        @Override
        protected void doFilterInternal(HttpServletRequest request, 
                                        HttpServletResponse response, 
                                        FilterChain filterChain) throws ServletException, IOException {
            String uri = request.getRequestURI();
            log.debug("JWT Filter - URI: {}", uri);
            
            // 从请求头中获取token
            String token = getTokenFromRequest(request);

            // 验证token
            if (StringUtils.hasText(token) && jwtUtil.validateToken(token)) {
                log.debug("Token 有效");
                
                // 获取用户信息
                Long userId = jwtUtil.getUserIdFromToken(token);
                String username = jwtUtil.getUsernameFromToken(token);
                
                // 创建Authentication对象并设置到SecurityContext
                UsernamePasswordAuthenticationToken authentication = 
                    new UsernamePasswordAuthenticationToken(
                        username, 
                        null, 
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))
                    );
                authentication.setDetails(userId);
                SecurityContextHolder.getContext().setAuthentication(authentication);
                
                log.debug("设置Authentication: {}", username);
                
                // Token有效,继续处理请求
                filterChain.doFilter(request, response);
            } else {
                log.debug("Token无效: {}", uri);
                // Token无效,返回401
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":401,\"message\":\"未授权或登录已过期\"}");
            }
        }

        /**
         * 从请求头中提取Token
         */
        private String getTokenFromRequest(HttpServletRequest request) {
            String bearerToken = request.getHeader("Authorization");
            if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
                return bearerToken.substring(7);
            }
            return null;
        }

        /**
         * 判断是否为公开路径
         */
        private boolean isPublicPath(String uri) {
            // 支持带/api前缀和不带前缀的路径
            return uri.equals("/user/login") || uri.equals("/user/test-password") || 
                   uri.equals("/api/user/login") || uri.equals("/api/user/test-password") ||
                   uri.startsWith("/ws/") || uri.startsWith("/terminal/") ||
                   uri.startsWith("/business/");
        }
    }
}
