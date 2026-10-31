package com.ambdulghaffar.springstarterapi.controller.ai;

import com.ambdulghaffar.springstarterapi.dto.AiDto;
import com.ambdulghaffar.springstarterapi.services.ai.AiChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/v1/ai")
@Tag(name = "AI Assistant", description = "Chat avec l'assistant IA (lecture seule sur la base).")
@RequiredArgsConstructor
public class AiChatController {

    private final AiChatService chatService;

    @Operation(
            summary = "Discuter avec l'assistant IA",
            description = "Envoie l'historique de la conversation (role = user ou assistant) "
                    + "et éventuellement le provider à utiliser (groq, gemini, ...). "
                    + "Si aucun provider n'est précisé, le provider par défaut est utilisé. "
                    + "L'IA répond en général ou consulte la base en lecture seule.")
    @PostMapping("/chat")
    public AiDto.ChatResponse chat(@RequestBody AiDto.ChatRequest request) {
        return chatService.chat(request);
    }

    @Operation(
            summary = "Discuter avec l'assistant IA en streaming",
            description = "Envoie l'historique de la conversation (role = user ou assistant) "
                    + "et éventuellement le provider à utiliser (groq, gemini, ...). "
                    + "Si aucun provider n'est précisé, le provider par défaut est utilisé. "
                    + "L'IA répond en général ou consulte la base en lecture seule.")
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStream(@RequestBody AiDto.ChatRequest request) {
        return chatService.chatStream(request);
    }
}