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
    // 저장된 image 키를 CloudFront 공개 URL로 변환한 값. controller가 채운다(DB 컬럼 아님).
    // 템플릿에서 @s3Service.publicUrl(...)을 직접 호출하면 Thymeleaf 3.1 표현식 제한으로
    // 500이 나므로(2026-08-18 실측), bean 호출을 controller로 옮기고 템플릿은 이 값만 읽는다.
    private String imageUrl;
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
