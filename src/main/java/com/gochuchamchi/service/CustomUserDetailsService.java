package com.gochuchamchi.service;

import com.gochuchamchi.dto.UserDto;
import com.gochuchamchi.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserMapper userMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 로그인 폼이 username(아이디)을 받으므로 findByUsername으로 조회
        UserDto user = userMapper.findByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + username);
        }
        String role = user.getRole() == null ? "user" : user.getRole();
        return new User(
            user.getUsername(),  // principal name을 username으로 통일
            user.getPassword(),
            true,                        // enabled
            true,                        // accountNonExpired
            true,                        // credentialsNonExpired
            !user.isSuspended(),         // accountNonLocked — 정지된 계정은 LockedException 으로 로그인 차단
            List.of(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
        );
    }
}
