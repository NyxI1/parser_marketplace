package org.example.parser.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.*;
import jakarta.persistence.Column;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Product {
    private Boolean notified;
    @Id
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    private long id;
    private long userId;
    private String title;
    @Column(columnDefinition = "TEXT")
    private String url;
    private Double targetPrice;
    private Double lastPrice;
}