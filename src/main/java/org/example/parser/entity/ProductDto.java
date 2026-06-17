package org.example.parser.entity;

import lombok.Data;

@Data
public class ProductDto {
    private String url;
    private Long userId;
    private Double targetPrice;
}