package com.gochuchamchi.mapper;

import com.gochuchamchi.dto.UserDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMapper {
    UserDto findByUsername(@Param("username") String username);
    UserDto findByEmail(@Param("email") String email);
    UserDto findById(@Param("id") Long id);
    void insert(UserDto user);
    void update(UserDto user);
    void updatePassword(@Param("id") Long id, @Param("password") String password);
    boolean existsByUsername(@Param("username") String username);
    boolean existsByEmail(@Param("email") String email);
}
