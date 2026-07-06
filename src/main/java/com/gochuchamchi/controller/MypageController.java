package com.gochuchamchi.controller;

import com.gochuchamchi.dto.UserDto;
import com.gochuchamchi.mapper.UserMapper;
import com.gochuchamchi.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/mypage")
@RequiredArgsConstructor
public class MypageController {

    private final UserService userService;
    private final UserMapper userMapper;

    @GetMapping
    public String mypage(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        model.addAttribute("user", userMapper.findByUsername(userDetails.getUsername()));
        return "mypage/index";
    }

    @GetMapping("/edit")
    public String editPage(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        model.addAttribute("user", userMapper.findByUsername(userDetails.getUsername()));
        return "mypage/edit";
    }

    @PostMapping("/edit")
    public String edit(@AuthenticationPrincipal UserDetails userDetails,
                       @RequestParam String name,
                       @RequestParam(required = false) String email,
                       @RequestParam String birthdate,
                       @RequestParam String phone,
                       @RequestParam String gender,
                       @RequestParam String nationality,
                       @RequestParam(required = false) String address,
                       @RequestParam(required = false) String newPassword,
                       @RequestParam(required = false) String confirmPassword,
                       RedirectAttributes ra) {
        UserDto user = userMapper.findByUsername(userDetails.getUsername());
        try {
            userService.updateProfile(user.getId(), name, email, birthdate, phone, gender, nationality, address);
            if (newPassword != null && !newPassword.isBlank()) {
                userService.updatePassword(user.getId(), null, newPassword, confirmPassword);
            }
            ra.addFlashAttribute("success", "회원 정보 변경이 완료되었습니다.");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/mypage/edit";
        }
        return "redirect:/mypage";
    }
}
