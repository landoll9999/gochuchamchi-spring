package com.gochuchamchi.config;

import com.gochuchamchi.mapper.UserMapper;
import com.gochuchamchi.service.AdminUserService;
import com.gochuchamchi.service.AuditLogService;
import com.gochuchamchi.service.CustomUserDetailsService;
import com.gochuchamchi.service.UserBehaviorLogService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final LoginFailureHandler loginFailureHandler;
    private final AuditAuthenticationSuccessHandler authenticationSuccessHandler;
    private final AuditLogoutSuccessHandler logoutSuccessHandler;
    private final AuditAccessDeniedHandler accessDeniedHandler;
    private final UserMapper userMapper;
    private final AdminUserService adminUserService;
    private final UserBehaviorLogService behaviorLogService;
    private final AuditLogService auditLogService;

    public SecurityConfig(CustomUserDetailsService userDetailsService,
                          LoginFailureHandler loginFailureHandler,
                          AuditAuthenticationSuccessHandler authenticationSuccessHandler,
                          AuditLogoutSuccessHandler logoutSuccessHandler,
                          AuditAccessDeniedHandler accessDeniedHandler,
                          UserMapper userMapper,
                          AdminUserService adminUserService,
                          UserBehaviorLogService behaviorLogService,
                          AuditLogService auditLogService) {
        this.userDetailsService = userDetailsService;
        this.loginFailureHandler = loginFailureHandler;
        this.authenticationSuccessHandler = authenticationSuccessHandler;
        this.logoutSuccessHandler = logoutSuccessHandler;
        this.accessDeniedHandler = accessDeniedHandler;
        this.userMapper = userMapper;
        this.adminUserService = adminUserService;
        this.behaviorLogService = behaviorLogService;
        this.auditLogService = auditLogService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/auth/**", "/shop/**", "/notice/**",
                                 "/css/**", "/js/**", "/images/**", "/error").permitAll()
                .requestMatchers("/admin/**").hasAnyRole("ADMIN", "SUPERADMIN")
                .requestMatchers("/mypage/**").authenticated()
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/auth/login")
                .loginProcessingUrl("/auth/login")
                .usernameParameter("username")
                .passwordParameter("password")
                .successHandler(authenticationSuccessHandler)
                .failureHandler(loginFailureHandler)   // 정지 계정이면 안내 문구를 세션에 담아 로그인 페이지로
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/auth/logout")
                .logoutSuccessHandler(logoutSuccessHandler)
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            .exceptionHandling(ex -> ex
                .accessDeniedHandler(accessDeniedHandler)
            )
            .userDetailsService(userDetailsService)
            // 로그인해 있는 동안 정지되면 다음 요청에서 바로 끊는다.
            // (필터를 @Component 로 두면 서블릿 체인에도 자동 등록되어 두 번 도므로 여기서 직접 만든다)
            .addFilterAfter(new SuspendedUserFilter(userMapper, adminUserService, auditLogService), AuthorizationFilter.class)
            .addFilterAfter(new UserBehaviorLoggingFilter(behaviorLogService), SuspendedUserFilter.class);
        return http.build();
    }
}
