package com.gochuchamchi.service;

import com.gochuchamchi.dto.UserDto;
import com.gochuchamchi.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdminUserDetailsServiceTest {

    private final UserMapper userMapper = mock(UserMapper.class);
    private final LoginAttemptService loginAttemptService = mock(LoginAttemptService.class);
    private final AdminUserDetailsService service =
            new AdminUserDetailsService(userMapper, loginAttemptService);

    @Test
    void acceptsAdminAccount() {
        UserDto user = user("manager", "admin");
        when(userMapper.findByUsername("manager")).thenReturn(user);

        var details = service.loadUserByUsername("manager");

        assertThat(details.getAuthorities())
                .extracting(Object::toString)
                .containsExactly("ROLE_ADMIN");
    }

    @Test
    void rejectsExistingNonAdminWithoutDisclosingIt() {
        when(userMapper.findByUsername("member")).thenReturn(user("member", "user"));

        assertThatThrownBy(() -> service.loadUserByUsername("member"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("관리자 계정을 찾을 수 없습니다");
    }

    private UserDto user(String username, String role) {
        UserDto user = new UserDto();
        user.setUsername(username);
        user.setPassword("{noop}password");
        user.setRole(role);
        return user;
    }
}
