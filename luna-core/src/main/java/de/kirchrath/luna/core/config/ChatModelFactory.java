package de.kirchrath.luna.core.config;

import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Component;

/** Builds the chat model of a configured provider; several providers can be used at the same time. */
@Component
public class ChatModelFactory {

    public ChatModel create(AiProperties.Provider provider) {
        return switch (provider.type()) {
            case OLLAMA -> {
                var api = OllamaApi.builder();
                if (provider.baseUrl() != null) {
                    api.baseUrl(provider.baseUrl());
                }
                yield OllamaChatModel.builder()
                        .ollamaApi(api.build())
                        .options(OllamaChatOptions.builder().model(provider.model()).build())
                        .build();
            }
            case ANTHROPIC -> AnthropicChatModel.builder()
                    .options(AnthropicChatOptions.builder()
                            .baseUrl(provider.baseUrl())
                            .apiKey(provider.apiKey())
                            .model(provider.model())
                            .build())
                    .build();
            case OPENAI -> OpenAiChatModel.builder()
                    .options(OpenAiChatOptions.builder()
                            .baseUrl(provider.baseUrl())
                            .apiKey(provider.apiKey())
                            .model(provider.model())
                            .build())
                    .build();
        };
    }
}
