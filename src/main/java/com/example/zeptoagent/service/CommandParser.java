package com.example.zeptoagent.service;

import com.example.zeptoagent.model.Item;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class CommandParser {
    private static final Map<String, Integer> WORDS = new HashMap<>();

    static {
        WORDS.put("one", 1);
        WORDS.put("a", 1);
        WORDS.put("an", 1);
        WORDS.put("two", 2);
        WORDS.put("three", 3);
        WORDS.put("four", 4);
        WORDS.put("five", 5);
        WORDS.put("six", 6);
        WORDS.put("seven", 7);
        WORDS.put("eight", 8);
        WORDS.put("nine", 9);
        WORDS.put("ten", 10);
    }

    public List<Item> parse(String input) {
        if (input == null || input.isBlank()) return List.of();

        String s = input.toLowerCase(Locale.ROOT)
                .replaceAll("\\b(order|buy|get|please|i want|i need|add)\\b", " ")
                .replaceAll("\\s+", " ")
                .trim();

        List<Item> result = new ArrayList<>();

        for (String part : s.split("\\s*(?:,|\\band\\b)\\s*")) {
            if (part.isBlank()) continue;

            int quantity = extractQuantity(part);
            String unit = part.matches(".*\\b(kg|kilogram|kilograms)\\b.*") ? "kg" : "item";

            String name = part.replaceAll("\\b\\d+\\b", " ");
            for (String word : WORDS.keySet()) {
                name = name.replaceAll("\\b" + Pattern.quote(word) + "\\b", " ");
            }

            name = name.replaceAll(
                    "\\b(kg|kilogram|kilograms|g|gram|grams|packet|packets|pack)\\b",
                    " "
            ).replaceAll("\\s+", " ").trim();

            if (!name.isBlank()) result.add(new Item(name, quantity, unit));
        }

        return result;
    }

    private int extractQuantity(String part) {
        Matcher matcher = Pattern.compile("\\b(\\d+)\\b").matcher(part);
        if (matcher.find()) {
            try {
                return Math.max(1, Math.min(100, Integer.parseInt(matcher.group(1))));
            } catch (NumberFormatException ignored) {
                return 1;
            }
        }

        for (var entry : WORDS.entrySet()) {
            if (part.matches(".*\\b" + Pattern.quote(entry.getKey()) + "\\b.*")) {
                return entry.getValue();
            }
        }
        return 1;
    }
}
