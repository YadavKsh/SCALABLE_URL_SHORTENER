package com.urlshortener.backend.service;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Service
public class UrlShortenerService {

    private final Map<String, String> shortToLong = new HashMap<>();
    private final Map<String, String> longToShort = new HashMap<>();
    private static final String CHARSET = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int BASE = CHARSET.length();

    public String shortenOriginalUrl(String originalUrl) {

        // ✅ If already exists, return existing short code
        if (longToShort.containsKey(originalUrl)) {
            return longToShort.get(originalUrl);
        }

        String shortCode = generateShortCode();

        while (shortToLong.containsKey(shortCode)) {
            shortCode = generateShortCode();
        }

        shortToLong.put(shortCode, originalUrl);
        longToShort.put(originalUrl, shortCode);

        return shortCode;
    }

    public String getOriginalUrl(String shortCode) {
        return shortToLong.get(shortCode);
    }

    private String generateShortCode() {
        StringBuilder shortCode = new StringBuilder();
        Random random = new Random();

        for (int i = 0; i < 6; i++) {
            shortCode.append(CHARSET.charAt(random.nextInt(BASE)));
        }

        return shortCode.toString();
    }
}