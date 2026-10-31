package com.ambdulghaffar.springstarterapi.dto;

import java.util.List;

public class AiDto {

    public record ChatMessage(String role, String content) {}

    public record ChatRequest(
            String provider,          // "groq", "gemini", ... nullable
            List<ChatMessage> messages
    ) {}

    public record ChatResponse(String provider, String content) {}
}
