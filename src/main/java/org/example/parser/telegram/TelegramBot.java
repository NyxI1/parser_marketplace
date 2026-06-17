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
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.ArrayList;
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
                    Привет! Я бот для отслеживания цен 🛒
                    
                           Что я умею:
                           ➕ добавлять товар по ссылке
                           📋 показывать список товаров
                           🔍 проверять цены
                           🗑 очищать список
                    
                           Для добавления товара отправь:
                            /add ссылка | целеваяЦена
            """);
        } else if (text.equals("➕ Добавить товар")) {
            send(chatId, "Отправь товар в формате:\n/add ссылка | целеваяЦена");
        } else if (text.equals("📋 Мои товары")) {
            listProducts(chatId);
        } else if (text.equals("🔍 Проверить цены")) {
            checkPrices(chatId);
        } else if (text.equals("🗑 Очистить список")) {
            productService.deleteProductsByUserId(chatId);
            send(chatId, "Список товаров очищен.");
        } else if (text.startsWith("/add")) {
            addProduct(chatId, text);
        } else {
            send(chatId, "Неизвестная команда. Напиши /start");
        }
    }

    private void addProduct(Long chatId, String text) {
        try {
            String data = text.replaceFirst("/add", "").trim();
            String[] parts = data.split("\\|");

            if (parts.length < 2) {
                send(chatId, "Формат: /add ссылка | целеваяЦена");
                return;
            }

            ProductDto dto = new ProductDto();
            dto.setUrl(parts[0].trim());
            dto.setUserId(chatId);
            dto.setTargetPrice(Double.parseDouble(parts[1].trim()));

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
        List<Product> products = productService.getProductsByUserId(chatId);

        if (products.isEmpty()) {
            send(chatId, "Список товаров пуст.");
            return;
        }

        StringBuilder sb = new StringBuilder("Товары для отслеживания:\n\n");

        for (Product product : products) {
            sb.append("📦 ").append(product.getTitle()).append("\n")
                    .append("💰 Текущая: ").append(product.getLastPrice()).append(" ₽\n")
                    .append("🎯 Целевая: ").append(product.getTargetPrice()).append(" ₽\n")
                    .append("🆔 ID: ").append(product.getId()).append("\n\n");
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

    private ReplyKeyboardMarkup mainKeyboard() {
        KeyboardRow row1 = new KeyboardRow();
        row1.add("➕ Добавить товар");
        row1.add("📋 Мои товары");

        KeyboardRow row2 = new KeyboardRow();
        row2.add("🔍 Проверить цены");
        row2.add("🗑 Очистить список");

        List<KeyboardRow> keyboard = new ArrayList<>();
        keyboard.add(row1);
        keyboard.add(row2);

        return ReplyKeyboardMarkup.builder()
                .keyboard(keyboard)
                .resizeKeyboard(true)
                .build();
    }

    public void send(Long chatId, String text) {
        try {
            SendMessage message = SendMessage.builder()
                    .chatId(chatId)
                    .text(text)
                    .replyMarkup(mainKeyboard())
                    .build();

            telegramClient.execute(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
