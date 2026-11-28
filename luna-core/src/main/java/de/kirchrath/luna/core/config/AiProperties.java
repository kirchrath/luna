package de.kirchrath.luna.core.config;

import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import de.kirchrath.luna.core.data.entity.LifeArea;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * @param providers the AI providers by their name
 * @param areas     the name of the provider that analyzes the inputs of each life area; every area needs one
 */
@Validated
@ConfigurationProperties("app.ai")
public record AiProperties(Map<String, @Valid Provider> providers, Map<LifeArea, String> areas) {

    public AiProperties {
        providers = providers == null ? Map.of() : Map.copyOf(providers);
        areas = areas == null ? Map.of() : Map.copyOf(areas);
    }

    /**
     * @param type    the API of the provider
     * @param baseUrl the address of the API; the default address of the type if empty
     * @param model   the chat model
     * @param apiKey  the API key; not needed for Ollama
     */
    public record Provider(@NotNull Type type, String baseUrl, @NotBlank String model, String apiKey) {

        public Provider {
            // The stack passes an unset base URL as empty variable.
            baseUrl = baseUrl == null || baseUrl.isBlank() ? null : baseUrl;
        }
    }

    public enum Type {
        OLLAMA,
        ANTHROPIC,
        OPENAI
    }
}
