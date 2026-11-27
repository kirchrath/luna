package de.kirchrath.luna.core.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.validation.autoconfigure.ValidationAutoConfiguration;

import de.kirchrath.luna.core.data.entity.LifeArea;
import de.kirchrath.luna.core.outbound.AreaChatClients;

class AiConfigTest {

    private final ChatModelFactory chatModelFactory = chatModelFactory();
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ValidationAutoConfiguration.class))
            .withUserConfiguration(AiConfig.class)
            .withBean(ChatModelFactory.class, () -> chatModelFactory)
            .withPropertyValues(
                    "app.ai.providers.local.type=ollama",
                    "app.ai.providers.local.model=llama3",
                    "app.ai.providers.cloud.type=anthropic",
                    "app.ai.providers.cloud.model=claude",
                    "app.ai.providers.cloud.base-url=",
                    "app.ai.providers.cloud.api-key=secret");

    @Test
    void thatAProviderForEveryAreaShouldResultIntoAChatClientPerArea() {
        // when
        contextRunner.withPropertyValues("app.ai.areas.PERSONAL=cloud", "app.ai.areas.WORK=local").run(context -> {
            // then
            assertThat(context).hasNotFailed();
            var clients = context.getBean(AreaChatClients.class);
            assertThat(clients.of(LifeArea.PERSONAL)).isNotNull();
            assertThat(clients.of(LifeArea.WORK)).isNotNull();
            verify(chatModelFactory).create(new AiProperties.Provider(AiProperties.Type.OLLAMA, null, "llama3", null));
            verify(chatModelFactory).create(
                    new AiProperties.Provider(AiProperties.Type.ANTHROPIC, null, "claude", "secret"));
        });
    }

    @Test
    void thatAnAreaWithoutProviderShouldResultIntoAFailedStart() {
        // when
        contextRunner.withPropertyValues("app.ai.areas.PERSONAL=cloud").run(context -> {
            // then
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).rootCause().hasMessageContaining("app.ai.areas.WORK");
        });
    }

    @Test
    void thatAnUnknownProviderShouldResultIntoAFailedStart() {
        // when
        contextRunner.withPropertyValues("app.ai.areas.PERSONAL=cloud", "app.ai.areas.WORK=office").run(context -> {
            // then
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).rootCause().hasMessageContaining("app.ai.providers.office");
        });
    }

    @Test
    void thatAProviderWithoutModelShouldResultIntoAFailedStart() {
        // when
        contextRunner.withPropertyValues("app.ai.providers.empty.type=openai", "app.ai.areas.PERSONAL=cloud",
                "app.ai.areas.WORK=local").run(context -> {
            // then
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).rootCause().hasMessageContaining("providers[empty].model");
        });
    }

    private static ChatModelFactory chatModelFactory() {
        var factory = mock(ChatModelFactory.class);
        when(factory.create(any())).thenReturn(mock(ChatModel.class));
        return factory;
    }
}
