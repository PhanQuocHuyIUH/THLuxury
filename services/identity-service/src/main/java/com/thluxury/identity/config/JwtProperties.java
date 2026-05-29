package com.thluxury.identity.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "thluxury.jwt")
public record JwtProperties(
        String issuer,
        String privateKeyPath,
        String publicKeyPath,
        long accessTtlMinutes,
        long refreshTtlDays
) {}
