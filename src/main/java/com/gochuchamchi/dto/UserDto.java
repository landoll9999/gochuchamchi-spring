package com.gochuchamchi.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UserDto {
    private Long id;
    private String username;
    private String name;
    private String email;
    private String password;
    private String phone;
    private String birthdate;
    private String gender;
    private String nationality;
    private String address;
    private String role;        // user, seller, admin
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
