package com.gochuchamchi.controller;

import com.gochuchamchi.dto.NoticeDto;
import com.gochuchamchi.dto.ProductDto;
import com.gochuchamchi.dto.UserDto;
import com.gochuchamchi.mapper.NoticeMapper;
import com.gochuchamchi.mapper.ProductMapper;
import com.gochuchamchi.mapper.UserMapper;
import com.gochuchamchi.service.AdminUserService;
import com.gochuchamchi.service.AuditLogService;
import com.gochuchamchi.service.S3Service;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final UserMapper userMapper;
    private final NoticeMapper noticeMapper;
    private final ProductMapper productMapper;
    private final AdminUserService adminUserService;
    private final AuditLogService auditLogService;
    private final S3Service s3Service;

    public AdminController(UserMapper userMapper, NoticeMapper noticeMapper,
                           ProductMapper productMapper, AdminUserService adminUserService,
                           AuditLogService auditLogService, S3Service s3Service) {
        this.userMapper = userMapper;
        this.noticeMapper = noticeMapper;
        this.productMapper = productMapper;
        this.adminUserService = adminUserService;
        this.auditLogService = auditLogService;
        this.s3Service = s3Service;
    }

    @GetMapping
    public String dashboard(Model model) {
        model.addAttribute("userCount", userMapper.findAllForAdmin().size());
        model.addAttribute("noticeCount", noticeMapper.findAllForAdmin().size());
        model.addAttribute("productCount", productMapper.countAll(""));
        return "admin/dashboard";
    }

    // ===================== 회원 관리 =====================

    @GetMapping("/users")
    public String users(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        UserDto me = actor(userDetails);
        List<UserDto> users = userMapper.findAllForAdmin();

        model.addAttribute("users", users);
        model.addAttribute("me", me);
        model.addAttribute("isSuperAdmin", AdminUserService.isSuperAdmin(me));
        // 화면에서 행마다 다시 계산하지 않도록 관리 가능한 id 를 미리 담아준다
        model.addAttribute("manageableIds", users.stream()
                .filter(u -> AdminUserService.canManage(me, u))
                .map(UserDto::getId)
                .toList());
        return "admin/users";
    }

    @PostMapping("/users/{id}/role")
    public String updateRole(@AuthenticationPrincipal UserDetails userDetails,
                             @PathVariable Long id,
                             @RequestParam String role,
                             RedirectAttributes ra) {
        try {
            adminUserService.changeRole(actor(userDetails), id, role);
            ra.addFlashAttribute("success", "권한이 변경되었습니다.");
        } catch (IllegalArgumentException e) {
            UserDto actor = actor(userDetails);
            auditLogService.failure("USER_ROLE_CHANGED", actor.getId(), actor.getUsername(),
                    "USER", String.valueOf(id), "POLICY_REJECTED", "requestedRole=" + role);
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/suspend")
    public String suspendUser(@AuthenticationPrincipal UserDetails userDetails,
                              @PathVariable Long id,
                              @RequestParam(defaultValue = "0") long days,
                              @RequestParam(defaultValue = "0") long hours,
                              @RequestParam(defaultValue = "0") long minutes,
                              @RequestParam(defaultValue = "0") long seconds,
                              @RequestParam(defaultValue = "false") boolean permanent,
                              RedirectAttributes ra) {
        try {
            adminUserService.suspend(actor(userDetails), id, days, hours, minutes, seconds, permanent);
            ra.addFlashAttribute("success", permanent
                    ? "해당 계정을 영구 정지했습니다."
                    : "해당 계정을 정지했습니다.");
        } catch (IllegalArgumentException e) {
            UserDto actor = actor(userDetails);
            auditLogService.failure("USER_SUSPENDED", actor.getId(), actor.getUsername(),
                    "USER", String.valueOf(id), "POLICY_REJECTED", "permanent=" + permanent);
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/unsuspend")
    public String unsuspendUser(@AuthenticationPrincipal UserDetails userDetails,
                                @PathVariable Long id,
                                RedirectAttributes ra) {
        try {
            adminUserService.unsuspend(actor(userDetails), id);
            ra.addFlashAttribute("success", "계정 정지를 해제했습니다.");
        } catch (IllegalArgumentException e) {
            UserDto actor = actor(userDetails);
            auditLogService.failure("USER_UNSUSPENDED", actor.getId(), actor.getUsername(),
                    "USER", String.valueOf(id), "POLICY_REJECTED", null);
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    // ===================== 공지사항 =====================

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
            auditLogService.successForUsername("NOTICE_CREATED", userDetails.getUsername(),
                    "NOTICE", String.valueOf(notice.getId()), "pinned=" + notice.isPinned());
            ra.addFlashAttribute("success", "공지사항이 등록되었습니다.");
        } else {
            noticeMapper.update(notice);
            auditLogService.successForUsername("NOTICE_UPDATED", userDetails.getUsername(),
                    "NOTICE", String.valueOf(notice.getId()), "pinned=" + notice.isPinned());
            ra.addFlashAttribute("success", "공지사항이 수정되었습니다.");
        }
        return "redirect:/admin/notices";
    }

    @PostMapping("/notices/{id}/delete")
    public String noticeDelete(@PathVariable Long id,
                               @AuthenticationPrincipal UserDetails userDetails,
                               RedirectAttributes ra) {
        noticeMapper.delete(id);
        auditLogService.successForUsername("NOTICE_DELETED", userDetails.getUsername(),
                "NOTICE", String.valueOf(id), null);
        ra.addFlashAttribute("success", "공지사항이 삭제되었습니다.");
        return "redirect:/admin/notices";
    }

    // ===================== 상품 =====================

    @GetMapping("/products")
    public String products(Model model) {
        List<ProductDto> products = productMapper.findAll(0, 100, "newest", "");
        products.forEach(p -> p.setImageUrl(s3Service.publicUrl(p.getImage())));
        model.addAttribute("products", products);
        return "admin/products";
    }

    @PostMapping("/products/{id}/delete")
    public String productDelete(@PathVariable Long id,
                                @AuthenticationPrincipal UserDetails userDetails,
                                RedirectAttributes ra) {
        productMapper.delete(id);
        auditLogService.successForUsername("PRODUCT_DELETED", userDetails.getUsername(),
                "PRODUCT", String.valueOf(id), null);
        ra.addFlashAttribute("success", "상품이 삭제되었습니다.");
        return "redirect:/admin/products";
    }

    /** long 범위를 넘는 값은 컨트롤러 진입 전에 변환 실패한다. 오류 페이지 대신 안내로 바꿔준다 */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public String handleBadNumber(MethodArgumentTypeMismatchException e, RedirectAttributes ra) {
        String field = e.getName();
        if (List.of("days", "hours", "minutes", "seconds").contains(field)) {
            ra.addFlashAttribute("error",
                "정지 기간에 넣을 수 없는 값입니다. 일은 최대 " + AdminUserService.MAX_SUSPEND_DAYS
                + "일(약 100년), 시간 · 분 · 초는 각각 " + AdminUserService.MAX_SUSPEND_UNIT + " 이하로 입력해주세요.");
            return "redirect:/admin/users";
        }
        ra.addFlashAttribute("error", "요청 값이 올바르지 않습니다. (" + field + ")");
        return "redirect:/admin";
    }

    /** 지금 로그인한 관리자 계정. */
    private UserDto actor(UserDetails userDetails) {
        return userMapper.findByUsername(userDetails.getUsername());
    }
}
