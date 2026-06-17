package org.example.parser.parser;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PriceParserService {

    public ParsedProduct parseProduct(String url) {
        try {
            Document document = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120 Safari/537.36")
                    .referrer("https://www.google.com")
                    .timeout(10000)
                    .get();

            String html = document.html();

            String title = document.title();
            Double price = extractPrice(html);

            if (title == null || title.isBlank()) {
                title = "Товар";
            }

            return new ParsedProduct(title, price);

        } catch (Exception e) {
            return new ParsedProduct("Товар", null);
        }
    }

    private Double extractPrice(String html) {
        Pattern ozonCardPrice = Pattern.compile("\"cardPrice\"\\s*:\\s*\"?(\\d+)");
        Matcher ozonCardPriceMatcher = ozonCardPrice.matcher(html);

        if (ozonCardPriceMatcher.find()) {
            return Double.parseDouble(ozonCardPriceMatcher.group(1));
        }

        Pattern pricePattern = Pattern.compile("\"price\"\\s*:\\s*\"?(\\d+)");
        Matcher priceMatcher = pricePattern.matcher(html);

        if (priceMatcher.find()) {
            return Double.parseDouble(priceMatcher.group(1));
        }

        Pattern rublePattern = Pattern.compile("(\\d{2,3}\\s?\\d{3})\\s*₽");
        Matcher rubleMatcher = rublePattern.matcher(html);

        if (rubleMatcher.find()) {
            String price = rubleMatcher.group(1).replace(" ", "");
            return Double.parseDouble(price);
        }

        return null;
    }
}
