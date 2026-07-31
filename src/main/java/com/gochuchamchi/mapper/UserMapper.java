package com.gochuchamchi.mapper;

import com.gochuchamchi.dto.UserDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.time.LocalDateTime;
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

    /** 계정 정지. permanent=true 면 until 은 무시된다. */
    void suspend(@Param("id") Long id,
                 @Param("until") LocalDateTime until,
                 @Param("permanent") boolean permanent,
                 @Param("actor") String actor);

    /** 계정 정지 해제. */
    void unsuspend(@Param("id") Long id);
}
