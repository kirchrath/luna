package de.kirchrath.luna.core.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.DynamicPropertyRegistrar;

/** One mocked chat model as provider of all life areas. */
@TestConfiguration(proxyBeanMethods = false)
public class ChatModelTestConfig {

    private static final String PROVIDER = "test";

    @Bean
    ChatModel chatModel() {
        var chatModel = mock(ChatModel.class);
        stubOptions(chatModel);
        return chatModel;
    }

    @Bean
    @Primary
    ChatModelFactory testChatModelFactory(ChatModel chatModel) {
        var factory = mock(ChatModelFactory.class);
        when(factory.create(any())).thenReturn(chatModel);
        return factory;
    }

    @Bean
    DynamicPropertyRegistrar aiProviderProperties() {
        return registry -> {
            registry.add("app.ai.providers." + PROVIDER + ".type", () -> "openai");
            registry.add("app.ai.providers." + PROVIDER + ".model", () -> "test");
            registry.add("app.ai.areas.PERSONAL", () -> PROVIDER);
            registry.add("app.ai.areas.WORK", () -> PROVIDER);
        };
    }

    public static void stubOptions(ChatModel chatModel) {
        when(chatModel.getOptions()).thenReturn(ChatOptions.builder().build());
    }
}
