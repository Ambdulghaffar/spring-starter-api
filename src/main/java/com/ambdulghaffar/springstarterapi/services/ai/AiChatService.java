package com.ambdulghaffar.springstarterapi.services.ai;

import com.ambdulghaffar.springstarterapi.dto.AiDto;
import com.ambdulghaffar.springstarterapi.properties.AiProperties;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

@Service
public class AiChatService {

    private final Map<String, ChatClient> chatClients;
    private final DbTools tools;
    private final String defaultProvider;

    public AiChatService(Map<String, ChatClient> chatClients, DbTools tools, AiProperties props) {
        this.chatClients = chatClients;
        this.tools = tools;
        this.defaultProvider = props.defaultProvider();
    }

    public AiDto.ChatResponse chat(AiDto.ChatRequest request) {
        String provider = (request.provider() == null || request.provider().isBlank())
                ? defaultProvider
                : request.provider();

        ChatClient client = chatClients.get(provider);
        if (client == null) {
            throw new IllegalArgumentException("Provider inconnu: " + provider
                    + ". Providers disponibles: " + chatClients.keySet());
        }

        List<Message> messages = request.messages().stream()
                .<Message>map(m -> "assistant".equals(m.role())
                        ? new AssistantMessage(m.content())
                        : new UserMessage(m.content()))
                .toList();

        String content = client.prompt().messages(messages).tools(tools).call().content();
        return new AiDto.ChatResponse(provider, content);
    }

    public Flux<String> chatStream(AiDto.ChatRequest request) {
        String provider = (request.provider() == null || request.provider().isBlank())
                ? defaultProvider
                : request.provider();

        ChatClient client = chatClients.get(provider);
        if (client == null) {
            throw new IllegalArgumentException("Provider inconnu: " + provider
                    + ". Providers disponibles: " + chatClients.keySet());
        }

        List<Message> messages = request.messages().stream()
                .<Message>map(m -> "assistant".equals(m.role())
                        ? new AssistantMessage(m.content())
                        : new UserMessage(m.content()))
                .toList();

        return client.prompt().messages(messages).tools(tools).stream().content();
    }
}