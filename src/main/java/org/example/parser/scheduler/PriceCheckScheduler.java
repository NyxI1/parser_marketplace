package org.example.parser.scheduler;

import lombok.RequiredArgsConstructor;
import org.example.parser.entity.Product;
import org.example.parser.service.ProductService;
import org.example.parser.telegram.TelegramBot;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.example.parser.repository.ProductRepository;
import org.example.parser.parser.PriceParserService;
import org.example.parser.parser.ParsedProduct;

import java.util.List;

@Component
@RequiredArgsConstructor
public class PriceCheckScheduler {

    private final ProductService productService;
    private final TelegramBot telegramBot;
    private final ProductRepository productRepository;
    private final PriceParserService priceParserService;

    @Scheduled(fixedRate = 60000)
    @Scheduled(fixedRate = 60000)
    public void checkPrices() {
        List<Product> products = productService.getAllProducts();

        for (Product product : products) {
            ParsedProduct parsed = priceParserService.parseProduct(product.getUrl());

            Double newPrice = parsed.getPrice();

            if (newPrice == null) {
                continue;
            }

            Double oldPrice = product.getLastPrice();

            product.setLastPrice(newPrice);

            if (newPrice <= product.getTargetPrice()
                    && !Boolean.TRUE.equals(product.getNotified())) {

                telegramBot.send(product.getUserId(), """
                    🔔 Цена достигла целевой!

                    Товар: %s
                    Старая цена: %.2f
                    Новая цена: %.2f
                    Целевая цена: %.2f
                    Ссылка: %s
                    """.formatted(
                        product.getTitle(),
                        oldPrice,
                        newPrice,
                        product.getTargetPrice(),
                        product.getUrl()
                ));

                product.setNotified(true);
            }

            productRepository.save(product);
        }
    }
}
