package de.kirchrath.luna.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;

@ConfigurationProperties("app.prompts")
public record PromptProperties(
        Resource classification,
        Resource task,
        Resource alert,
        Resource appointment,
        Resource invoice,
        Resource contract,
        Resource note) {
}
