package de.kirchrath.luna.core.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.openai.OpenAiChatModel;

class ChatModelFactoryTest {

    private final ChatModelFactory chatModelFactory = new ChatModelFactory();

    @Test
    void thatAnOllamaProviderShouldResultIntoAnOllamaModelWithItsModelName() {
        // given
        var provider = new AiProperties.Provider(AiProperties.Type.OLLAMA, "http://ollama:11434", "llama3", null);

        // when
        var model = chatModelFactory.create(provider);

        // then
        assertThat(model).isInstanceOf(OllamaChatModel.class);
        assertThat(model.getOptions().getModel()).isEqualTo("llama3");
    }

    @Test
    void thatAnOllamaProviderWithoutBaseUrlShouldResultIntoAnOllamaModel() {
        // given
        var provider = new AiProperties.Provider(AiProperties.Type.OLLAMA, null, "llama3", null);

        // when
        var model = chatModelFactory.create(provider);

        // then
        assertThat(model).isInstanceOf(OllamaChatModel.class);
    }

    @Test
    void thatAnAnthropicProviderShouldResultIntoAnAnthropicModelWithItsModelName() {
        // given
        var provider = new AiProperties.Provider(AiProperties.Type.ANTHROPIC, null, "claude-test", "key");

        // when
        var model = chatModelFactory.create(provider);

        // then
        assertThat(model).isInstanceOf(AnthropicChatModel.class);
        assertThat(model.getOptions().getModel()).isEqualTo("claude-test");
    }

    @Test
    void thatAnOpenAiProviderShouldResultIntoAnOpenAiModelWithItsModelName() {
        // given
        var provider = new AiProperties.Provider(AiProperties.Type.OPENAI, "http://ai-stub:8000", "stub", "key");

        // when
        var model = chatModelFactory.create(provider);

        // then
        assertThat(model).isInstanceOf(OpenAiChatModel.class);
        assertThat(model.getOptions().getModel()).isEqualTo("stub");
    }
}
