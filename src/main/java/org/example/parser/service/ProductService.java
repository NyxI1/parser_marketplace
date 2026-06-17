package org.example.parser.service;

import lombok.RequiredArgsConstructor;
import org.example.parser.entity.Product;
import org.example.parser.entity.ProductDto;
import org.example.parser.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.example.parser.parser.PriceParserService;
import org.example.parser.parser.ParsedProduct;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final PriceParserService priceParserService;

    public Product createProduct(ProductDto product) {

        ParsedProduct parsed = priceParserService.parseProduct(product.getUrl());

        Double currentPrice = parsed.getPrice();

        if (currentPrice == null) {
            currentPrice = product.getLastPrice();
        }

        Product db = Product.builder()
                .url(product.getUrl())
                .title(parsed.getTitle())
                .userId(product.getUserId())
                .targetPrice(product.getTargetPrice())
                .lastPrice(currentPrice)
                .build();

        return productRepository.save(db);
    }

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public void deleteAllProducts() {
        productRepository.deleteAll();
    }
}
