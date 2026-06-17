package org.example.parser.telegram;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.example.parser.entity.Product;
import org.example.parser.entity.ProductDto;
import org.example.parser.service.ProductService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.List;

@Component
@RequiredArgsConstructor
public class TelegramBot implements LongPollingSingleThreadUpdateConsumer {

    private final ProductService productService;

    @Value("${telegram.bot.token}")
    private String botToken;

    private TelegramClient telegramClient;
    private TelegramBotsLongPollingApplication botsApplication;

    @PostConstruct
    public void init() throws Exception {
        telegramClient = new OkHttpTelegramClient(botToken);
        botsApplication = new TelegramBotsLongPollingApplication();
        botsApplication.registerBot(botToken, this);
    }

    @PreDestroy
    public void shutdown() throws Exception {
        if (botsApplication != null) {
            botsApplication.close();
        }
    }

    @Override
    public void consume(Update update) {
        if (!update.hasMessage() || !update.getMessage().hasText()) {
            return;
        }

        Long chatId = update.getMessage().getChatId();
        String text = update.getMessage().getText();

        if (text.equals("/start")) {
            send(chatId, """
                    Привет! Я бот для отслеживания цен.

                    Команды:
                    /add название | ссылка | целеваяЦена | текущаяЦена
                    /list
                    /clear
                    /check
                    """);
        } else if (text.startsWith("/add")) {
            addProduct(chatId, text);
        } else if (text.equals("/list")) {
            listProducts(chatId);
        } else if (text.equals("/clear")) {
            productService.deleteAllProducts();
            send(chatId, "Список товаров очищен.");
        } else if (text.equals("/check")) {
            checkPrices(chatId);
        } else {
            send(chatId, "Неизвестная команда. Напиши /start");
        }
    }

    private void addProduct(Long chatId, String text) {
        try {
            String data = text.replaceFirst("/add", "").trim();
            String[] parts = data.split("\\|");

            if (parts.length < 3) {
                send(chatId, "Формат: /add ссылка | целеваяЦена | текущаяЦена");
                return;
            }

            ProductDto dto = new ProductDto();
            dto.setUrl(parts[0].trim());
            dto.setUserId(chatId);
            dto.setTargetPrice(Double.parseDouble(parts[1].trim()));
            dto.setLastPrice(Double.parseDouble(parts[2].trim()));

            Product saved = productService.createProduct(dto);

            send(chatId, "Товар добавлен:\n" +
                    saved.getTitle() + "\n" +
                    "Текущая цена: " + saved.getLastPrice() + "\n" +
                    "Целевая цена: " + saved.getTargetPrice());


        } catch (Exception e) {
            send(chatId, "Ошибка при добавлении товара. Формат: /add ссылка | целеваяЦена");
        }
    }

    private void listProducts(Long chatId) {
        List<Product> products = productService.getAllProducts();

        if (products.isEmpty()) {
            send(chatId, "Список товаров пуст.");
            return;
        }

        StringBuilder sb = new StringBuilder("Товары для отслеживания:\n\n");

        for (Product product : products) {
            sb.append("ID: ").append(product.getId()).append("\n")
                    .append("Название: ").append(product.getTitle()).append("\n")
                    .append("Ссылка: ").append(product.getUrl()).append("\n")
                    .append("Целевая цена: ").append(product.getTargetPrice()).append("\n")
                    .append("Текущая цена: ").append(product.getLastPrice()).append("\n\n");
        }

        send(chatId, sb.toString());
    }

    private void checkPrices(Long chatId) {
        List<Product> products = productService.getAllProducts();

        boolean found = false;

        for (Product product : products) {
            if (product.getLastPrice() <= product.getTargetPrice()) {
                found = true;
                send(chatId, "Цена снизилась!\n" +
                        product.getTitle() + "\n" +
                        "Текущая цена: " + product.getLastPrice() + "\n" +
                        "Целевая цена: " + product.getTargetPrice());
            }
        }

        if (!found) {
            send(chatId, "Пока нет товаров со сниженной ценой.");
        }
    }

    public void send(Long chatId, String text) {
        try {
            SendMessage message = SendMessage.builder()
                    .chatId(chatId)
                    .text(text)
                    .build();

            telegramClient.execute(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
