package com.gochuchamchi.controller;

import com.gochuchamchi.dto.ProductDto;
import com.gochuchamchi.dto.ProductSizeDto;
import com.gochuchamchi.dto.UserDto;
import com.gochuchamchi.mapper.ProductMapper;
import com.gochuchamchi.mapper.ProductSizeMapper;
import com.gochuchamchi.mapper.UserMapper;
import com.gochuchamchi.service.S3Service;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
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
    private static final int PAGE_SIZE = 12;

    public ShopController(ProductMapper productMapper, ProductSizeMapper productSizeMapper,
                          UserMapper userMapper, S3Service s3Service) {
        this.productMapper = productMapper;
        this.productSizeMapper = productSizeMapper;
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
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    public String registerForm() {
        return "shop/register";
    }

    @PostMapping("/register")
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    public String register(@RequestParam String brand,
                           @RequestParam String name,
                           @RequestParam String category,
                           @RequestParam int price,
                           @RequestParam(defaultValue = "0") int stock,
                           @RequestParam(required = false) String description,
                           @RequestParam(required = false) MultipartFile imageFile,
                           @RequestParam(required = false) List<String> sizes,
                           @AuthenticationPrincipal UserDetails userDetails) throws Exception {
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
            product.setImage(s3Service.upload(imageFile));
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

        return "redirect:/shop";
    }
}