package com.urlshortener.backend.service;

import com.urlshortener.backend.model.Url;
import com.urlshortener.backend.repository.UrlRepository;
import org.springframework.stereotype.Service;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Optional;
import java.util.Random;

@Service
public class UrlShortenerService {

    private final UrlRepository repository;
    //private final StringRedisTemplate redisTemplate;
    private static final String CHARSET = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int BASE = CHARSET.length();

    public UrlShortenerService(UrlRepository repository) {
        this.repository = repository;
        //this.redisTemplate = redisTemplate;
    }

    public String shortenUrl(String originalUrl) {

        // ✅ Check duplicate
        Optional<Url> existing = repository.findByOriginalUrl(originalUrl);
        if (existing.isPresent()) {
            return existing.get().getShortCode();
        }

        String shortCode = generateShortCode();

        // ✅ Handle collision
        while (repository.findByShortCode(shortCode).isPresent()) {
            shortCode = generateShortCode();
        }

        Url url = new Url(shortCode, originalUrl);
        repository.save(url);
        // 🔥 Cache it
        //redisTemplate.opsForValue().set(shortCode, originalUrl);

        return shortCode;
    }

    public String getOriginalUrl(String shortCode) {

        // 🔥 1. Check Redis first
        String cachedUrl = null;
                // redisTemplate.opsForValue().get(shortCode);

        if (cachedUrl != null) {
            System.out.println("⚡ Served from Redis");
            return cachedUrl;
        }

        // 🔹 2. Fallback to DB
        Optional<Url> url = repository.findByShortCode(shortCode);

        if (url.isPresent()) {
            String originalUrl = url.get().getOriginalUrl();

            // 🔥 3. Store in Redis
            //redisTemplate.opsForValue().set(shortCode, originalUrl);

            System.out.println("💾 Fetched from DB and cached");

            return originalUrl;
        }

        return null;
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