package com.ambdulghaffar.springstarterapi.dto;

import com.ambdulghaffar.springstarterapi.entity.Media;
import com.ambdulghaffar.springstarterapi.entity.enums.MediaType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public final class MediaDto {

    private MediaDto() {}

    @Schema(name = "MediaRegisterRequest")
    public record RegisterRequest(
            @NotBlank @Size(max = 500) String url,
            @NotBlank @Size(max = 200) String publicId,
            @NotNull MediaType type
    ) {}

    @Schema(name = "MediaUploadSignature")
    public record UploadSignature(String signature, long timestamp, String apiKey,
                                  String cloudName, String folder) {}

    @Schema(name = "MediaResponse")
    public record Response(
            Long id,
            String url,
            String publicId,
            MediaType type,
            boolean inUse,
            LocalDateTime createdAt
    ) {
        public static Response from(Media media, boolean inUse) {
            return new Response(media.getId(), media.getUrl(), media.getPublicId(),
                    media.getType(), inUse, media.getCreatedAt());
        }
    }

    @Schema(name = "MediaDeleteResult")
    public record DeleteResult(int deleted, int skippedInUse, int failed) {}
}