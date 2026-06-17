package org.example.parser.controlers;

import lombok.RequiredArgsConstructor;
import org.example.parser.entity.Product;
import org.example.parser.entity.ProductDto;
import org.example.parser.repository.ProductRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping ("/api/product")
public class ProductController {

    private final ProductRepository productRepository;

    @PostMapping("/create")
    public Product createProduct(@RequestBody ProductDto product) {

        Product bd = Product.builder()
                .url(product.getUrl())
                .title(product.getTitle())
                .targetPrice(product.getTargetPrice())
                .lastPrice(product.getLastPrice())
                .build();

        return productRepository.save(bd);
    }
}
