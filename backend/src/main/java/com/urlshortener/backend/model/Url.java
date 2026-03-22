package com.urlshortener.backend.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
public class Url {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String shortCode;

    @Column(unique = true, columnDefinition = "TEXT")
    private String originalUrl;

    // Constructors
    public Url() {}

    public Url(String shortCode, String originalUrl) {
        this.shortCode = shortCode;
        this.originalUrl = originalUrl;
    }
}