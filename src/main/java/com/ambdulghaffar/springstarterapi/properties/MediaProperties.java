package com.ambdulghaffar.springstarterapi.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

@ConfigurationProperties(prefix = "media")
public record MediaProperties(
        @DefaultValue("7d") Duration orphanRetention
) {}