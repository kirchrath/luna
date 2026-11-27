package de.kirchrath.luna.core.config;

import java.util.EnumMap;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import de.kirchrath.luna.core.data.entity.LifeArea;
import de.kirchrath.luna.core.outbound.AreaChatClients;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AiProperties.class)
public class AiConfig {

    /** One chat client per life area; luna-core does not start if an area has no or an unknown provider. */
    @Bean
    AreaChatClients areaChatClients(AiProperties aiProperties, ChatModelFactory chatModelFactory) {
        var clients = new EnumMap<LifeArea, ChatClient>(LifeArea.class);
        for (var area : LifeArea.values()) {
            var name = aiProperties.areas().get(area);
            if (name == null) {
                throw new IllegalStateException("No AI provider configured for life area " + area
                        + " (app.ai.areas." + area + ")");
            }
            var provider = aiProperties.providers().get(name);
            if (provider == null) {
                throw new IllegalStateException("Unknown AI provider '" + name + "' for life area " + area
                        + " (app.ai.providers." + name + ")");
            }
            clients.put(area, ChatClient.builder(chatModelFactory.create(provider)).build());
        }
        return new AreaChatClients(clients);
    }
}
