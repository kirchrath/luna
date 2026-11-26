package de.kirchrath.luna.appointment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.websocket")
public record WebSocketProperties(String endpoint, String brokerPrefix, Topics topics) {

    public record Topics(String appointments) {
    }
}
