package com.ambdulghaffar.springstarterapi.config.jwt;

import com.ambdulghaffar.springstarterapi.properties.JwtProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("jwt")
@EnableConfigurationProperties(JwtProperties.class)
public class JwtConfig {
}