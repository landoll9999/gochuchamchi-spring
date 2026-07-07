package com.gochuchamchi.controller;
import com.gochuchamchi.dto.ProductDto;
import com.gochuchamchi.dto.UserDto;
import com.gochuchamchi.mapper.ProductMapper;
import com.gochuchamchi.mapper.UserMapper;
import com.gochuchamchi.service.S3Service;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Controller
@RequestMapping("/shop")
public class ShopController {

    private final ProductMapper productMapper;
    private final UserMapper userMapper;
    private final S3Service s3Service;
    private static final int PAGE_SIZE = 12;

    public ShopController(ProductMapper productMapper, UserMapper userMapper, S3Service s3Service) {
        this.productMapper = productMapper;
        this.userMapper = userMapper;
        this.s3Service = s3Service;
    }

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

    @GetMapping("/register-form")
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    public String registerForm() {
        return "shop/register";
    }

    @PostMapping("/register")
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    public String register(@ModelAttribute ProductDto product,
                           @RequestParam(required = false) MultipartFile imageFile,
                           @AuthenticationPrincipal UserDetails userDetails) throws Exception {
        UserDto seller = userMapper.findByUsername(userDetails.getUsername());
        product.setSellerId(seller.getId());
        if (imageFile != null && !imageFile.isEmpty()) {
            product.setImage(s3Service.upload(imageFile));
        }
        productMapper.insert(product);
        return "redirect:/shop";
    }
}