package com.gochuchamchi.controller;

import com.gochuchamchi.dto.ProductDto;
import com.gochuchamchi.dto.ProductSizeDto;
import com.gochuchamchi.dto.UserDto;
import com.gochuchamchi.mapper.ProductMapper;
import com.gochuchamchi.mapper.ProductSizeMapper;
import com.gochuchamchi.mapper.UserMapper;
import com.gochuchamchi.service.S3Service;
import com.gochuchamchi.service.AuditLogService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/shop")
public class ShopController {

    private final ProductMapper productMapper;
    private final ProductSizeMapper productSizeMapper;
    private final UserMapper userMapper;
    private final S3Service s3Service;
    private final AuditLogService auditLogService;
    private static final int PAGE_SIZE = 12;

    /** 정렬 드롭다운에서 허용하는 값. 이 밖의 값이 들어오면 기본값(신상품순)으로 되돌린다. */
    private static final List<String> SORT_OPTIONS =
            List.of("newest", "views_desc", "views_asc", "price_asc", "price_desc", "name", "brand");
    private static final String DEFAULT_SORT = "newest";

    public ShopController(ProductMapper productMapper, ProductSizeMapper productSizeMapper,
                          UserMapper userMapper, S3Service s3Service,
                          AuditLogService auditLogService) {
        this.productMapper = productMapper;
        this.productSizeMapper = productSizeMapper;
        this.userMapper = userMapper;
        this.s3Service = s3Service;
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public String list(@RequestParam(defaultValue = DEFAULT_SORT) String sort,
                       @RequestParam(defaultValue = "") String category,
                       @RequestParam(defaultValue = "1") int page,
                       Model model) {
        sort = normalizeSort(sort);
        if (page < 1) page = 1;

        int offset = (page - 1) * PAGE_SIZE;
        int total = productMapper.countAll(category);
        List<ProductDto> products = productMapper.findAll(offset, PAGE_SIZE, sort, category);
        products.forEach(p -> p.setImageUrl(s3Service.publicUrl(p.getImage())));
        model.addAttribute("products", products);
        model.addAttribute("totalCount", total);
        model.addAttribute("totalPages", (int) Math.ceil((double) total / PAGE_SIZE));
        model.addAttribute("currentPage", page);
        model.addAttribute("sort", sort);
        model.addAttribute("category", category);
        return "shop/list";
    }

    /** 이전 링크(popular)를 새 값으로 옮겨주고, 알 수 없는 값은 기본 정렬로 되돌린다. */
    private String normalizeSort(String sort) {
        if ("popular".equals(sort)) return "views_desc";   // 기존 '인기상품' 링크 호환
        return SORT_OPTIONS.contains(sort) ? sort : DEFAULT_SORT;
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        ProductDto product = productMapper.findById(id);
        if (product == null) return "redirect:/shop";
        product.setImageUrl(s3Service.publicUrl(product.getImage()));
        productMapper.incrementViews(id);
        List<ProductSizeDto> sizes = productSizeMapper.findByProductId(id);
        product.setSizes(sizes.stream().map(s -> {
            ProductDto.SizeDto sd = new ProductDto.SizeDto();
            sd.setName(s.getSizeName());
            sd.setSoldOut(s.isSoldOut());
            return sd;
        }).collect(java.util.stream.Collectors.toList()));
        model.addAttribute("product", product);
        return "shop/detail";
    }

    @GetMapping("/register-form")
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN', 'SUPERADMIN')")
    public String registerForm() {
        return "shop/register";
    }

    @PostMapping("/register")
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN', 'SUPERADMIN')")
    public String register(@RequestParam String brand,
                           @RequestParam String name,
                           @RequestParam String category,
                           @RequestParam int price,
                           @RequestParam(defaultValue = "0") int stock,
                           @RequestParam(required = false) String description,
                           @RequestParam(required = false) MultipartFile imageFile,
                           @RequestParam(required = false) List<String> sizes,
                           @AuthenticationPrincipal UserDetails userDetails,
                           RedirectAttributes redirectAttributes) throws Exception {
        UserDto seller = userMapper.findByUsername(userDetails.getUsername());

        ProductDto product = new ProductDto();
        product.setSellerId(seller.getId());
        product.setBrand(brand);
        product.setName(name);
        product.setCategory(category);
        product.setPrice(price);
        product.setStock(stock);
        product.setDescription(description);

        if (imageFile != null && !imageFile.isEmpty()) {
            try {
                product.setImage(s3Service.upload(imageFile));
            } catch (IllegalArgumentException e) {
                redirectAttributes.addFlashAttribute("error", e.getMessage());
                return "redirect:/shop/register-form";
            }
        }
        productMapper.insert(product);

        if (sizes != null && !sizes.isEmpty()) {
            List<ProductSizeDto> sizeList = new ArrayList<>();
            for (int i = 0; i < sizes.size(); i++) {
                ProductSizeDto sd = new ProductSizeDto();
                sd.setProductId(product.getId());
                sd.setSizeName(sizes.get(i));
                sd.setStock(0);
                sd.setSortOrder(i);
                sizeList.add(sd);
            }
            productSizeMapper.insertSizes(sizeList);
        }

        auditLogService.success("PRODUCT_CREATED", seller.getId(), seller.getUsername(),
                "PRODUCT", String.valueOf(product.getId()), "category=" + category);

        return "redirect:/shop";
    }
}
