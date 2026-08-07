package com.gochuchamchi.service;

import com.gochuchamchi.dto.RegisterForm;
import com.gochuchamchi.dto.UserDto;
import com.gochuchamchi.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    public UserDto findById(Long id) {
        return userMapper.findById(id);
    }

    public void register(RegisterForm form) {
        // 아이디 중복 체크
        if (userMapper.existsByUsername(form.getUsername())) {
            throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
        }
        // 이메일 중복 체크 (이메일 입력 시에만)
        if (form.getEmail() != null && !form.getEmail().isBlank()
                && userMapper.existsByEmail(form.getEmail())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }
        if (!form.getPassword().equals(form.getConfirmPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        UserDto user = new UserDto();
        user.setUsername(form.getUsername());
        user.setName(form.getName());
        user.setEmail(form.getEmail());
        user.setPassword(passwordEncoder.encode(form.getPassword()));
        user.setPhone(form.getPhone());
        user.setBirthdate(form.getBirthdate());
        user.setGender(form.getGender());
        user.setNationality(form.getNationality());
        user.setAddress(form.getAddress());
        userMapper.insert(user);
        auditLogService.success("USER_REGISTERED", user.getId(), user.getUsername(),
                "USER", String.valueOf(user.getId()), null);
    }

    public void updateProfile(Long id, String name, String email, String birthdate,
                              String phone, String gender, String nationality, String address) {
        UserDto user = userMapper.findById(id);
        user.setName(name);
        user.setEmail(email);
        user.setBirthdate(birthdate);
        user.setPhone(phone);
        user.setGender(gender);
        user.setNationality(nationality);
        user.setAddress(address);
        userMapper.update(user);
        auditLogService.success("PROFILE_UPDATED", user.getId(), user.getUsername(),
                "USER", String.valueOf(user.getId()), null);
    }

    public void updatePassword(Long id, String currentPassword, String newPassword, String confirmPassword) {
        // currentPassword가 null이면 (회원정보 변경에서 호출) 현재 비밀번호 검증 생략
        if (currentPassword != null) {
            UserDto user = userMapper.findById(id);
            if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
                throw new IllegalArgumentException("현재 비밀번호가 올바르지 않습니다.");
            }
        }
        if (!newPassword.equals(confirmPassword)) {
            throw new IllegalArgumentException("새 비밀번호가 일치하지 않습니다.");
        }
        if (newPassword.length() < 8) {
            throw new IllegalArgumentException("비밀번호는 8자 이상이어야 합니다.");
        }
        userMapper.updatePassword(id, passwordEncoder.encode(newPassword));
        UserDto user = userMapper.findById(id);
        auditLogService.success("PASSWORD_CHANGED", id, user == null ? null : user.getUsername(),
                "USER", String.valueOf(id), null);
    }
}
