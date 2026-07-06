package com.gochuchamchi.controller;

import com.gochuchamchi.dto.NoticeDto;
import com.gochuchamchi.dto.ProductDto;
import com.gochuchamchi.dto.UserDto;
import com.gochuchamchi.mapper.NoticeMapper;
import com.gochuchamchi.mapper.ProductMapper;
import com.gochuchamchi.mapper.UserMapper;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final UserMapper userMapper;
    private final NoticeMapper noticeMapper;
    private final ProductMapper productMapper;

    public AdminController(UserMapper userMapper, NoticeMapper noticeMapper, ProductMapper productMapper) {
        this.userMapper = userMapper;
        this.noticeMapper = noticeMapper;
        this.productMapper = productMapper;
    }

    // 대시보드
    @GetMapping
    public String dashboard(Model model) {
        model.addAttribute("userCount", userMapper.findAllForAdmin().size());
        model.addAttribute("noticeCount", noticeMapper.findAllForAdmin().size());
        model.addAttribute("productCount", productMapper.countAll(""));
        return "admin/dashboard";
    }

    // 회원 관리
    @GetMapping("/users")
    public String users(Model model) {
        model.addAttribute("users", userMapper.findAllForAdmin());
        return "admin/users";
    }

    @PostMapping("/users/{id}/role")
    public String updateRole(@PathVariable Long id,
                             @RequestParam String role,
                             RedirectAttributes ra) {
        userMapper.updateRole(id, role);
        ra.addFlashAttribute("success", "권한이 변경되었습니다.");
        return "redirect:/admin/users";
    }

    // 공지사항 관리
    @GetMapping("/notices")
    public String notices(Model model) {
        model.addAttribute("notices", noticeMapper.findAllForAdmin());
        return "admin/notices";
    }

    @GetMapping("/notices/new")
    public String noticeNewForm(Model model) {
        model.addAttribute("notice", new NoticeDto());
        return "admin/notice-form";
    }

    @GetMapping("/notices/{id}/edit")
    public String noticeEditForm(@PathVariable Long id, Model model) {
        model.addAttribute("notice", noticeMapper.findById(id));
        return "admin/notice-form";
    }

    @PostMapping("/notices/save")
    public String noticeSave(@ModelAttribute NoticeDto notice,
                             @AuthenticationPrincipal UserDetails userDetails,
                             RedirectAttributes ra) {
        notice.setAuthor(userDetails.getUsername());
        if (notice.getId() == null) {
            noticeMapper.insert(notice);
            ra.addFlashAttribute("success", "공지사항이 등록되었습니다.");
        } else {
            noticeMapper.update(notice);
            ra.addFlashAttribute("success", "공지사항이 수정되었습니다.");
        }
        return "redirect:/admin/notices";
    }

    @PostMapping("/notices/{id}/delete")
    public String noticeDelete(@PathVariable Long id, RedirectAttributes ra) {
        noticeMapper.delete(id);
        ra.addFlashAttribute("success", "공지사항이 삭제되었습니다.");
        return "redirect:/admin/notices";
    }

    // 상품 관리
    @GetMapping("/products")
    public String products(Model model) {
        model.addAttribute("products", productMapper.findAll(0, 100, "newest", ""));
        return "admin/products";
    }

    @PostMapping("/products/{id}/delete")
    public String productDelete(@PathVariable Long id, RedirectAttributes ra) {
        productMapper.delete(id);
        ra.addFlashAttribute("success", "상품이 삭제되었습니다.");
        return "redirect:/admin/products";
    }
}
