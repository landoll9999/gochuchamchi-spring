package com.gochuchamchi.service;

import com.gochuchamchi.dto.UserDto;
import com.gochuchamchi.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
@Profile("admin")
@RequiredArgsConstructor
public class AdminUserDetailsService implements UserDetailsService {

    private static final Set<String> ADMIN_ROLES = Set.of(
            AdminUserService.ROLE_ADMIN,
            AdminUserService.ROLE_SUPERADMIN
    );

    private final UserMapper userMapper;
    private final LoginAttemptService loginAttemptService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        loginAttemptService.ensureNotThrottled(username);
        UserDto user = userMapper.findByUsername(username);
        if (user == null || !ADMIN_ROLES.contains(user.getRole())) {
            // Do not disclose whether the account exists or merely lacks an admin role.
            throw new UsernameNotFoundException("관리자 계정을 찾을 수 없습니다");
        }
        return new User(
                user.getUsername(),
                user.getPassword(),
                true,
                true,
                true,
                !user.isSuspended(),
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().toUpperCase()))
        );
    }
}
