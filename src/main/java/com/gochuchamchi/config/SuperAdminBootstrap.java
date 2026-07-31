package com.gochuchamchi.config;

import com.gochuchamchi.dto.UserDto;
import com.gochuchamchi.mapper.UserMapper;
import com.gochuchamchi.service.AdminUserService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 지정한 아이디를 기동 시 superadmin 으로 승격.
 *
 * 지정 방법: 환경변수 APP_SUPERADMIN_USERNAME (권장) / 설정의 app.superadmin.username / 환경변수 SUPERADMIN_USERNAME.
 * 컨테이너는 --spring.config.location 으로 외부 yml 을 통째로 쓰므로, 설정 파일과 무관한 환경변수 방식이 안전하다.
 *
 * 비어 있으면 아무 것도 안 함. 계정 생성은 하지 않으므로 회원가입이 먼저다.
 * 승격만 하고 강등은 안 하며, 이미 superadmin 이면 건너뛴다.
 */
@Component
@RequiredArgsConstructor
public class SuperAdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SuperAdminBootstrap.class);

    private final UserMapper userMapper;

    @Value("${app.superadmin.username:${SUPERADMIN_USERNAME:}}")
    private String superAdminUsername;

    @Override
    public void run(ApplicationArguments args) {
        if (superAdminUsername == null || superAdminUsername.isBlank()) {
            return;
        }
        String username = superAdminUsername.trim();

        UserDto user = userMapper.findByUsername(username);
        if (user == null) {
            log.warn("[SUPERADMIN] '{}' 계정을 찾을 수 없어 승격하지 못했습니다. 먼저 회원가입을 진행해주세요.", username);
            return;
        }
        if (AdminUserService.ROLE_SUPERADMIN.equals(user.getRole())) {
            log.info("[SUPERADMIN] '{}' 계정은 이미 superadmin 입니다.", username);
            return;
        }

        userMapper.updateRole(user.getId(), AdminUserService.ROLE_SUPERADMIN);
        log.warn("[SUPERADMIN] '{}' 계정의 권한을 '{}' → superadmin 으로 승격했습니다.", username, user.getRole());
    }
}
