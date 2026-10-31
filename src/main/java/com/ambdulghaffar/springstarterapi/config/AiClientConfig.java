package com.ambdulghaffar.springstarterapi.config;

import com.ambdulghaffar.springstarterapi.properties.AiProperties;
import com.openai.client.OpenAIClient;
import com.openai.client.OpenAIClientAsync;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.client.okhttp.OpenAIOkHttpClientAsync;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class AiClientConfig {

    @Bean
    public Map<String, ChatClient> chatClients(AiProperties props) {
        Map<String, ChatClient> clients = new HashMap<>();

        props.providers().forEach((name, cfg) -> {

            OpenAIClient openAiClient = OpenAIOkHttpClient.builder()
                    .baseUrl(cfg.baseUrl())
                    .apiKey(cfg.apiKey())
                    .build();

            OpenAIClientAsync openAiClientAsync = OpenAIOkHttpClientAsync.builder()
                    .baseUrl(cfg.baseUrl())
                    .apiKey(cfg.apiKey())
                    .build();

            OpenAiChatOptions options = OpenAiChatOptions.builder()
                    .model(cfg.model())
                    .build();

            OpenAiChatModel model = OpenAiChatModel.builder()
                    .openAiClient(openAiClient)
                    .openAiClientAsync(openAiClientAsync)
                    .options(options)
                    .build();

            clients.put(name, ChatClient.builder(model)
                    .defaultSystem("""
                            Tu es l'assistant du dashboard admin. Réponds en français.
                            Tu peux discuter de tout sujet. Pour les données du projet
                            (utilisateurs, posts, commentaires, adresses), utilise les outils
                            et n'invente jamais de chiffres.
                            """)
                    .build());
        });

        return clients;
    }
}