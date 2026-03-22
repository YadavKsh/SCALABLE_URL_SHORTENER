package com.urlshortener.backend.controller;

import com.urlshortener.backend.service.UrlShortenerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
public class UrlShortenerController {

    private final UrlShortenerService service;

    public UrlShortenerController(UrlShortenerService service) {
        this.service = service;
    }

    // 🔹 POST /shorten
    @PostMapping("/shorten")
    public ResponseEntity<?> shortenUrl(@RequestBody Map<String, String> request) {

        String originalUrl = request.get("url");

        if (originalUrl == null || originalUrl.isEmpty() || !isValidUrl(originalUrl)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid URL"));
        }

        String shortCode = service.shortenUrl(originalUrl);

        String shortUrl = "http://localhost:8081/" + shortCode;

        return ResponseEntity.ok(Map.of(
                "shortUrl", shortUrl,
                "shortCode", shortCode,
                "originalUrl", originalUrl
        ));
    }

    // 🔹 GET /{shortCode}
    @GetMapping("/{shortCode}")
    public ResponseEntity<?> redirect(@PathVariable String shortCode) {

        String originalUrl = service.getOriginalUrl(shortCode);

        if (originalUrl == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity
                .status(302)
                .location(URI.create(originalUrl))
                .build();
    }

    private boolean isValidUrl(String url) {
        try {
            new java.net.URL(url).toURI();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}