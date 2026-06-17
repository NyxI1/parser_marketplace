package org.example.parser.scheduler;

import lombok.RequiredArgsConstructor;
import org.example.parser.entity.Product;
import org.example.parser.service.ProductService;
import org.example.parser.telegram.TelegramBot;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.example.parser.repository.ProductRepository;

import java.util.List;

@Component
@RequiredArgsConstructor
public class PriceCheckScheduler {

    private final ProductService productService;
    private final TelegramBot telegramBot;
    private final ProductRepository productRepository;

    @Scheduled(fixedRate = 60000)
    public void checkPrices() {
        List<Product> products = productService.getAllProducts();

        for (Product product : products) {
            if (product.getLastPrice() != null
                    && product.getTargetPrice() != null
                    && product.getLastPrice() <= product.getTargetPrice()
                    && !Boolean.TRUE.equals(product.getNotified())) {

                telegramBot.send(product.getUserId(), """
                        🔔 Цена снизилась!

                        Товар: %s
                        Текущая цена: %.2f
                        Целевая цена: %.2f
                        Ссылка: %s
                        """.formatted(
                        product.getTitle(),
                        product.getLastPrice(),
                        product.getTargetPrice(),
                        product.getUrl()
                ));
                product.setNotified(true);
                productRepository.save(product);
            }
        }
    }
}
