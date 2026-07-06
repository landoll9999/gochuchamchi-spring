package com.gochuchamchi.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RegisterForm {

    @NotBlank(message = "아이디를 입력해주세요.")
    @Pattern(regexp = "^[a-zA-Z0-9]{6,20}$", message = "아이디는 영문+숫자 6~20자로 입력해주세요.")
    private String username;

    @NotBlank(message = "비밀번호를 입력해주세요.")
    @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*[0-9])(?=.*[!@#$%^&*]).{8,}$",
             message = "비밀번호는 8자 이상 영문+숫자+특수문자를 포함해야 합니다.")
    private String password;

    @NotBlank(message = "비밀번호 확인을 입력해주세요.")
    private String confirmPassword;

    @Email(message = "유효한 이메일 형식을 입력해주세요.")
    private String email;

    @NotBlank(message = "이름을 입력해주세요.")
    private String name;

    @NotBlank(message = "생년월일 8자리를 입력해주세요.")
    @Pattern(regexp = "^\\d{8}$", message = "생년월일은 8자리 숫자로 입력해주세요.")
    private String birthdate;

    @NotBlank(message = "휴대전화번호를 입력해주세요.")
    @Pattern(regexp = "^01[0-9]\\d{7,8}$", message = "올바른 휴대전화번호를 입력해주세요.")
    private String phone;

    private String gender = "M";
    private String nationality = "domestic";
    private String address;
}
