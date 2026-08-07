package com.gochuchamchi.config;

import com.gochuchamchi.dto.UserDto;
import com.gochuchamchi.mapper.UserMapper;
import com.gochuchamchi.service.AdminUserService;
import com.gochuchamchi.service.AuditLogService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 로그인 실패 처리. 정지된 계정은 accountNonLocked=false 라 LockedException 이 오는데,
 * 이때만 안내 문구를 세션에 담아 로그인 페이지에서 alert 으로 보여준다.
 */
@Component
public class LoginFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    /** 로그인 페이지가 한 번 읽고 지우는 세션 키 */
    public static final String SUSPENDED_MESSAGE = "SUSPENDED_MESSAGE";

    private final UserMapper userMapper;
    private final AdminUserService adminUserService;
    private final AuditLogService auditLogService;

    public LoginFailureHandler(UserMapper userMapper, AdminUserService adminUserService,
                               AuditLogService auditLogService) {
        this.userMapper = userMapper;
        this.adminUserService = adminUserService;
        this.auditLogService = auditLogService;
        setDefaultFailureUrl("/auth/login?error=true");
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception)
            throws IOException, ServletException {

        String username = request.getParameter("username");
        String reasonCode = exception instanceof LockedException
                ? "ACCOUNT_SUSPENDED" : "AUTHENTICATION_FAILED";
        auditLogService.failure("LOGIN", null, username, "USER", null, reasonCode, null);

        if (exception instanceof LockedException) {
            UserDto user = (username == null || username.isBlank()) ? null : userMapper.findByUsername(username);

            if (user != null && user.isSuspended()) {
                request.getSession().setAttribute(SUSPENDED_MESSAGE, adminUserService.suspensionMessage(user));
                getRedirectStrategy().sendRedirect(request, response, "/auth/login?suspended=true");
                return;
            }
        }

        super.onAuthenticationFailure(request, response, exception);
    }
}
