package com.gochuchamchi.controller;

import com.gochuchamchi.dto.ProductDto;
import com.gochuchamchi.dto.UserDto;
import com.gochuchamchi.mapper.ProductMapper;
import com.gochuchamchi.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Controller
@RequestMapping("/shop")
@RequiredArgsConstructor
public class ShopController {

    private final ProductMapper productMapper;
    private final UserMapper userMapper;
    private static final int PAGE_SIZE = 12;

    @GetMapping
    public String list(@RequestParam(defaultValue = "newest") String sort,
                       @RequestParam(defaultValue = "") String category,
                       @RequestParam(defaultValue = "1") int page,
                       Model model) {
        int offset = (page - 1) * PAGE_SIZE;
        int total = productMapper.countAll(category);
        model.addAttribute("products", productMapper.findAll(offset, PAGE_SIZE, sort, category));
        model.addAttribute("totalCount", total);
        model.addAttribute("totalPages", (int) Math.ceil((double) total / PAGE_SIZE));
        model.addAttribute("currentPage", page);
        model.addAttribute("sort", sort);
        model.addAttribute("category", category);
        return "shop/list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        ProductDto product = productMapper.findById(id);
        if (product == null) return "redirect:/shop";
        productMapper.incrementViews(id);
        model.addAttribute("product", product);
        return "shop/detail";
    }

    @PostMapping("/register")
    @PreAuthorize("hasRole('SELLER')")
    public String register(@ModelAttribute ProductDto product,
                           @RequestParam(required = false) MultipartFile imageFile,
                           @AuthenticationPrincipal UserDetails userDetails) {
        // seller_id 세팅 (누락 시 DB NOT NULL 오류)
        UserDto seller = userMapper.findByUsername(userDetails.getUsername());
        product.setSellerId(seller.getId());

        // 이미지 처리 (추후 S3 연동 시 여기에 업로드 로직 추가)
        // if (imageFile != null && !imageFile.isEmpty()) { ... }

        productMapper.insert(product);
        return "redirect:/shop";
    }
}
