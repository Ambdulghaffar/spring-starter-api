package com.ambdulghaffar.springstarterapi.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

@ConfigurationProperties(prefix = "ai")
public record AiProperties(
        String defaultProvider,
        Map<String, ProviderConfig> providers
) {

    public AiProperties {
        providers = providers != null ? providers : Map.of();
    }

    public record ProviderConfig(
            String baseUrl,
            String apiKey,
            String model
    ) {}
}