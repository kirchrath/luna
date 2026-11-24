package de.kirchrath.luna.core.config;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration(proxyBeanMethods = false)
public class ChatModelTestConfig {

    @Bean
    ChatModel chatModel() {
        var chatModel = mock(ChatModel.class);
        stubOptions(chatModel);
        return chatModel;
    }

    public static void stubOptions(ChatModel chatModel) {
        when(chatModel.getOptions()).thenReturn(ChatOptions.builder().build());
    }
}
