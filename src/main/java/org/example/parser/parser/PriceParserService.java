package org.example.parser.parser;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class PriceParserService {

    public ParsedProduct parseProduct(String url) {
        try {
            Document document = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("Accept-Language", "ru-RU,ru;q=0.9,en-US;q=0.8,en;q=0.7")
                    .header("Connection", "keep-alive")
                    .referrer("https://market.yandex.ru/")
                    .timeout(120000)
                    .maxBodySize(0)
                    .followRedirects(true)
                    .ignoreHttpErrors(true)
                    .get();

            Files.writeString(
                    Path.of("C:/Users/Никита/Downloads/parser/page.html"),
                    document.html()
            );

            String html = document.html();

            String title = extractTitle(document.html(), document.title());

            System.out.println("HTML LENGTH = " + html.length());

            Double price = extractPrice(html);

            System.out.println("PRICE = " + price);

            if (title == null || title.isBlank()) {
                title = "Товар";
            }

            return new ParsedProduct(title, price);

        } catch (Exception e) {
            System.out.println("ERROR TYPE = " + e.getClass().getName());
            System.out.println("ERROR MESSAGE = " + e.getMessage());
            e.printStackTrace();

            return new ParsedProduct("Товар", null);
        }
    }

    private String extractTitle(String html, String fallbackTitle) {
        String normalized = html
                .replace("\\\"", "\"")
                .replace("&quot;", "\"")
                .replace("&amp;", "&");

        Pattern productTitlePattern = Pattern.compile("\"title\"\\s*:\\s*\"([^\"]*iPhone[^\"]*)\"");
        Matcher productTitleMatcher = productTitlePattern.matcher(normalized);

        if (productTitleMatcher.find()) {
            return productTitleMatcher.group(1);
        }

        Pattern commonTitlePattern = Pattern.compile("\"modelName\"\\s*:\\s*\"([^\"]+)\"");
        Matcher commonTitleMatcher = commonTitlePattern.matcher(normalized);

        if (commonTitleMatcher.find()) {
            return commonTitleMatcher.group(1);
        }

        return fallbackTitle;
    }

    private Double extractPrice(String html) {
        String normalized = html
                .replace("\\\"", "\"")
                .replace("&quot;", "\"")
                .replace("&amp;", "&");

        Pattern actualPricePattern = Pattern.compile(
                "\"actualPrice\"\\s*:\\s*\\{\"amount\"\\s*:\\s*\\{\"intPart\"\\s*:\\s*\"(\\d+)\""
        );
        Matcher actualPriceMatcher = actualPricePattern.matcher(normalized);

        if (actualPriceMatcher.find()) {
            return Double.parseDouble(actualPriceMatcher.group(1));
        }

        Pattern priceValuePattern =
                Pattern.compile("\"priceValue\"\\s*:\\s*(\\d+)");
        Matcher priceValueMatcher = priceValuePattern.matcher(normalized);

        if (priceValueMatcher.find()) {
            return Double.parseDouble(priceValueMatcher.group(1));
        }

        Pattern valuePattern = Pattern.compile("\"price\"\\s*:\\s*\\{\\s*\"value\"\\s*:\\s*(\\d+)");
        Matcher valueMatcher = valuePattern.matcher(normalized);

        if (valueMatcher.find()) {
            return Double.parseDouble(valueMatcher.group(1));
        }

        return null;
    }
}
