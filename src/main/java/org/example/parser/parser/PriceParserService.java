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
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120 Safari/537.36")
                    .referrer("https://www.google.com")
                    .timeout(10000)
                    .get();

            Files.writeString(
                    Path.of("C:/Users/Никита/Downloads/parser/page.html"),
                    document.html()
            );

            String html = document.html();

            String title = extractTitle(document.html(), document.title());
            Double price = extractPrice(html);

            if (title == null || title.isBlank()) {
                title = "Товар";
            }

            return new ParsedProduct(title, price);

        } catch (Exception e) {
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
                "\"actualPrice\"\\s*:\\s*\\{\\s*\"amount\"\\s*:\\s*\\{\\s*\"intPart\"\\s*:\\s*(\\d+)"
        );
        Matcher actualPriceMatcher = actualPricePattern.matcher(normalized);

        if (actualPriceMatcher.find()) {
            return Double.parseDouble(actualPriceMatcher.group(1));
        }

        Pattern priceValuePattern = Pattern.compile("\"priceValue\"\\s*:\\s*\"(\\d+)\"");
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
