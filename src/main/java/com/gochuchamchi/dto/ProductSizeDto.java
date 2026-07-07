package com.gochuchamchi.dto;

import lombok.Data;

@Data
public class ProductSizeDto {
    private Long id;
    private Long productId;
    private String sizeName;
    private int stock;
    private int sortOrder;
    private boolean soldOut;
}