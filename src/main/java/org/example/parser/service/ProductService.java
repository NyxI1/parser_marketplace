package org.example.parser.service;

import lombok.RequiredArgsConstructor;
import org.example.parser.entity.Product;
import org.example.parser.entity.ProductDto;
import org.example.parser.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    public Product createProduct(ProductDto product) {
        Product db = Product.builder()
                .url(product.getUrl())
                .title(product.getTitle())
                .userId(product.getUserId())
                .targetPrice(product.getTargetPrice())
                .lastPrice(product.getLastPrice())
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
