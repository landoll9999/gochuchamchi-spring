package com.gochuchamchi.mapper;

import com.gochuchamchi.dto.UserDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface UserMapper {
    UserDto findByUsername(String username);
    UserDto findById(Long id);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    void insert(UserDto user);
    void update(UserDto user);
    void updatePassword(@Param("id") Long id, @Param("password") String password);
    List<UserDto> findAllForAdmin();
    void updateRole(@Param("id") Long id, @Param("role") String role);
}
