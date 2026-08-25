package com.gochuchamchi.config;

import com.gochuchamchi.logging.StructuredApplicationLogService;
import com.gochuchamchi.mapper.UserMapper;
import com.gochuchamchi.service.AdminUserDetailsService;
import com.gochuchamchi.service.AdminUserService;
import com.gochuchamchi.service.AuditLogService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@Profile("admin")
@EnableWebSecurity
@EnableMethodSecurity
public class AdminSecurityConfig {

    private final AdminUserDetailsService userDetailsService;
    private final LoginFailureHandler loginFailureHandler;
    private final AdminAuthenticationSuccessHandler authenticationSuccessHandler;
    private final AdminLogoutSuccessHandler logoutSuccessHandler;
    private final AuditAccessDeniedHandler accessDeniedHandler;
    private final UserMapper userMapper;
    private final AdminUserService adminUserService;
    private final AuditLogService auditLogService;
    private final StructuredApplicationLogService structuredApplicationLogService;

    public AdminSecurityConfig(AdminUserDetailsService userDetailsService,
                               LoginFailureHandler loginFailureHandler,
                               AdminAuthenticationSuccessHandler authenticationSuccessHandler,
                               AdminLogoutSuccessHandler logoutSuccessHandler,
                               AuditAccessDeniedHandler accessDeniedHandler,
                               UserMapper userMapper,
                               AdminUserService adminUserService,
                               AuditLogService auditLogService,
                               StructuredApplicationLogService structuredApplicationLogService) {
        this.userDetailsService = userDetailsService;
        this.loginFailureHandler = loginFailureHandler;
        this.authenticationSuccessHandler = authenticationSuccessHandler;
        this.logoutSuccessHandler = logoutSuccessHandler;
        this.accessDeniedHandler = accessDeniedHandler;
        this.userMapper = userMapper;
        this.adminUserService = adminUserService;
        this.auditLogService = auditLogService;
        this.structuredApplicationLogService = structuredApplicationLogService;
    }

    @Bean
    public SecurityFilterChain adminFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/internal/health", "/auth/login", "/css/**", "/js/**", "/images/**", "/error/**").permitAll()
                .requestMatchers("/admin/**").hasAnyRole("ADMIN", "SUPERADMIN")
                .anyRequest().denyAll()
            )
            .formLogin(form -> form
                .loginPage("/auth/login")
                .loginProcessingUrl("/auth/login")
                .usernameParameter("username")
                .passwordParameter("password")
                .successHandler(authenticationSuccessHandler)
                .failureHandler(loginFailureHandler)
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/auth/logout")
                .logoutSuccessHandler(logoutSuccessHandler)
                .invalidateHttpSession(true)
                .deleteCookies("ADMINSESSION")
                .permitAll()
            )
            .exceptionHandling(ex -> ex.accessDeniedHandler(accessDeniedHandler))
            .userDetailsService(userDetailsService)
            .addFilterBefore(new HttpAccessLoggingFilter(structuredApplicationLogService),
                    UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(new SuspendedUserFilter(userMapper, adminUserService, auditLogService),
                    AuthorizationFilter.class);
        return http.build();
    }
}
