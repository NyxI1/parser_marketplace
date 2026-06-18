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
import java.util.Set;
import java.util.HashSet;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class TelegramBot implements LongPollingSingleThreadUpdateConsumer {

    private final ProductService productService;
    private final Set<Long> waitingForDelete = new HashSet<>();

    @Value("${telegram.bot.token}")
    private String botToken;

    private TelegramClient telegramClient;
    private TelegramBotsLongPollingApplication botsApplication;

    @PostConstruct
    public void init() throws Exception {
        telegramClient = new OkHttpTelegramClient(botToken);
        botsApplication = new TelegramBotsLongPollingApplication();
        botsApplication.registerBot(botToken, this);
        System.out.println("✅ Telegram bot registered");
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

        if (waitingForDelete.contains(chatId)) {
            try {
                Long productId = Long.parseLong(text.trim());

                boolean deleted = productService.deleteProduct(productId, chatId);

                if (deleted) {
                    send(chatId, "🗑 Товар удалён.");
                } else {
                    send(chatId, "❌ Товар не найден.");
                }

            } catch (Exception e) {
                send(chatId, "⚠️ Введите только ID товара, например: 15");
            }

            waitingForDelete.remove(chatId);
            return;
        }

        if (text.equals("/start")) {
            send(chatId, """
🛒 Бот отслеживания цен

Добро пожаловать!

📌 Используйте кнопки ниже для управления товарами.
""");
        } else if (text.equals("➕ Добавить товар")) {
            send(chatId, "📦 Отправьте ссылку и целевую цену:\n\n" +
                    "Пример:\n" +
                    "https://market.yandex.ru/... | 50000");
        } else if (text.equals("📋 Мои товары")) {
            listProducts(chatId);
        } else if (text.equals("🔍 Проверить цены")) {
            checkPrices(chatId);
        } else if (text.equals("🧹 Очистить список")) {
            productService.deleteProductsByUserId(chatId);
            send(chatId, "Список товаров очищен.");
        } else if (text.startsWith("/add")) {
            addProduct(chatId, text);
        } else if (text.contains("market.yandex.ru")) {
            addProduct(chatId, text);
        } else if (text.equals("🗑 Удалить товар")) {
            waitingForDelete.add(chatId);
            send(chatId, "Введите ID товара, который нужно удалить:");
        } else if (text.startsWith("/delete")) {
            deleteProduct(chatId, text);
        } else if (text.equals("ℹ️ О проекте")) {
            send(chatId,
                    """
                    📊 Price Tracker Bot
                    
                    Telegram-бот для отслеживания цен на товары из Яндекс Маркета.
                    
                    ✅ Добавление товаров
                    ✅ Отслеживание цен
                    ✅ Уведомления о скидках
                    ✅ PostgreSQL
                    
                    🎞️ Презентация:
                    https://www.figma.com/make/oFKIN1SAJsALBSocfnmZsM/%D0%9F%D1%80%D0%B5%D0%B7%D0%B5%D0%BD%D1%82%D0%B0%D1%86%D0%B8%D1%8F-parser_marketplace?code-node-id=1-8&p=f&fullscreen=1
                    
                    👨‍💻 Автор:
                    Петров Никита
                    Группа 9/1-РПО-25/1
                    """);
        } else {
            send(chatId, "Неизвестная команда. Напиши /start");
        }
    }

    private void addProduct(Long chatId, String text) {
        try {
            String data = text.trim();
            String[] parts = data.split("\\|");

            if (parts.length < 2) {
                send(chatId,
                        "📦 Отправьте ссылку и целевую цену:\n\n" +
                                "Пример:\n" +
                                "https://market.yandex.ru/... | 50000");
                return;
            }

            ProductDto dto = new ProductDto();
            dto.setUrl(parts[0].trim());
            dto.setUserId(chatId);
            dto.setTargetPrice(Double.parseDouble(parts[1].trim()));

            Product saved = productService.createProduct(dto);

            send(chatId,
                    "🛒 Товар добавлен в отслеживание\n\n" +
                            "📦 Название:\n" +
                            saved.getTitle() + "\n\n" +
                            "💰 Текущая цена: " + saved.getLastPrice() + " ₽\n" +
                            "🎯 Желаемая цена: " + saved.getTargetPrice() + " ₽\n\n" +
                            "🔔 Вы получите уведомление при достижении целевой цены.");


        } catch (Exception e) {
            e.printStackTrace();
            send(chatId, "Ошибка: " + e.getMessage());
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
        List<Product> products =  productService.getProductsByUserId(chatId);

        boolean found = false;

        for (Product product : products) {
            if (product.getLastPrice() <= product.getTargetPrice()) {
                found = true;
                send(chatId,
                        "🎯 Целевая цена достигнута!\n" +
                                product.getTitle() + "\n" +
                                "💰 Текущая цена: " + product.getLastPrice() + "\n" +
                                "🎯 Целевая цена: " + product.getTargetPrice()
                );
            }
        }

        if (!found) {
            send(chatId, "Пока нет товаров со сниженной ценой.");
        }
    }

    private void deleteProduct(Long chatId, String text) {
        try {
            String idText = text.replace("/delete", "").trim();

            Long productId = Long.parseLong(idText);

            boolean deleted =
                    productService.deleteProduct(productId, chatId);

            if (deleted) {
                send(chatId, "Товар удалён.");
            } else {
                send(chatId, "Товар не найден.");
            }

        } catch (Exception e) {
            send(chatId, "Формат: /delete id");
        }
    }

    private ReplyKeyboardMarkup mainKeyboard() {
        KeyboardRow row1 = new KeyboardRow();
        row1.add("➕ Добавить товар");
        row1.add("📋 Мои товары");

        KeyboardRow row2 = new KeyboardRow();
        row2.add("🔍 Проверить цены");
        row2.add("🧹 Очистить список");

        KeyboardRow row3 = new KeyboardRow();
        row3.add("🗑 Удалить товар");

        KeyboardRow row4 = new KeyboardRow();
        row3.add("ℹ️ О проекте");

        List<KeyboardRow> keyboard = new ArrayList<>();
        keyboard.add(row1);
        keyboard.add(row2);
        keyboard.add(row3);

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
