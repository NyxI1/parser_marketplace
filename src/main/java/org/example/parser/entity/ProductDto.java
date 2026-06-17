package org.example.parser.entity;

import lombok.Data;

@Data
public class ProductDto {

    private String url;

    private String title;

    private Long userId;

    private String marketplace;

    private Double targetPrice;

    private Double lastPrice;
}