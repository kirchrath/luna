package de.kirchrath.luna.core.outbound;

import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;

import de.kirchrath.luna.core.data.entity.LifeArea;

/** The chat client of each life area, so that every input is analyzed by the provider of its area. */
public record AreaChatClients(Map<LifeArea, ChatClient> clients) {

    public AreaChatClients {
        clients = Map.copyOf(clients);
    }

    public ChatClient of(LifeArea area) {
        return clients.get(area);
    }
}
