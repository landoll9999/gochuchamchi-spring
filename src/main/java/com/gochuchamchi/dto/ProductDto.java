package com.gochuchamchi.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ProductDto {
    private Long id;
    private Long sellerId;
    private String brand;
    private String name;
    private String category;
    private int price;
    private int stock;
    private String image;
    private String description;
    private boolean newItem;     // isNew → newItem (Lombok 충돌 방지)
    private boolean active;      // isActive → active
    private int viewCount;
    private LocalDateTime createdAt;

    private List<SizeDto> sizes;
    private List<SizeGuideDto> sizeGuide;
    private List<String> images;

    @Data
    public static class SizeDto {
        private String name;
        private boolean soldOut;
    }

    @Data
    public static class SizeGuideDto {
        private String size;
        private String shoulder;
        private String chest;
        private String sleeve;
        private String length;
    }
}
