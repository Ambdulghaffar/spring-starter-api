package com.ambdulghaffar.springstarterapi.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "cloudinary")
public record CloudinaryProperties(
        String cloudName,
        String apiKey,
        String apiSecret,
        String rootFolder
) {
    public CloudinaryProperties {
        if (rootFolder == null || rootFolder.isBlank()) {
            throw new IllegalStateException(
                    "cloudinary.root-folder (CLOUDINARY_ROOT_FOLDER) est obligatoire, ex: social-media/local");
        }
        rootFolder = rootFolder.replaceAll("^/+|/+$", "");
    }
}