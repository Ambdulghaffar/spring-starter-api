package com.ambdulghaffar.springstarterapi.config;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.ambdulghaffar.springstarterapi.properties.CloudinaryProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CloudinaryConfig {

    @Bean
    public Cloudinary cloudinary(CloudinaryProperties properties) {
        return new Cloudinary(ObjectUtils.asMap(
                "cloud_name", properties.cloudName(),
                "api_key", properties.apiKey(),
                "api_secret", properties.apiSecret(),
                "secure", true
        ));
    }
}