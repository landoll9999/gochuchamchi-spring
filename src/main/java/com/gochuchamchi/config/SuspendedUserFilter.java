package com.gochuchamchi.config;

import com.gochuchamchi.dto.UserDto;
import com.gochuchamchi.mapper.UserMapper;
import com.gochuchamchi.service.AdminUserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 로그인 중에 정지되면 다음 요청에서 바로 끊는 필터.
 * 로그인 시점 차단만으로는 이미 열린 세션이 만료(1시간)까지 살아 있다.
 * 정적 리소스는 건너뛰므로 DB 조회는 페이지 요청에서만 일어난다.
 */
public class SuspendedUserFilter extends OncePerRequestFilter {

    private final UserMapper userMapper;
    private final AdminUserService adminUserService;

    public SuspendedUserFilter(UserMapper userMapper, AdminUserService adminUserService) {
        this.userMapper = userMapper;
        this.adminUserService = adminUserService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return path.startsWith("/css/") || path.startsWith("/js/") || path.startsWith("/images/")
            || path.equals("/favicon.ico") || path.startsWith("/auth/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            UserDto user = userMapper.findByUsername(auth.getName());

            if (user != null && user.isSuspended()) {
                String message = adminUserService.suspensionMessage(user);
                new SecurityContextLogoutHandler().logout(request, response, auth);  // 세션 무효화
                // 안내는 새 세션에 담아 로그인 페이지가 한 번 읽고 지운다
                request.getSession(true).setAttribute(LoginFailureHandler.SUSPENDED_MESSAGE, message);
                response.sendRedirect(request.getContextPath() + "/auth/login?suspended=true");
                return;
            }
        }

        chain.doFilter(request, response);
    }
}
