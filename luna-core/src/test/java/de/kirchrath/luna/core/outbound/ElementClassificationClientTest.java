package de.kirchrath.luna.core.outbound;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.core.io.ClassPathResource;

import de.kirchrath.luna.core.config.ChatModelTestConfig;
import de.kirchrath.luna.core.dto.LunaElementTypeDto;

class ElementClassificationClientTest {

    private final ChatModel chatModel = chatModel();
    private final ElementClassificationClient elementClassificationClient = new ElementClassificationClient(
            ChatClient.builder(chatModel), new ClassPathResource("prompts/classification.st"));

    @Test
    void thatClassifyingATextWithoutTypeHintShouldResultIntoTheTypesOfTheModel() {
        // given
        answer("{\"types\": [\"APPOINTMENT\", \"TASK\"]}");

        // when
        var result = elementClassificationClient.classify("Dentist on Tuesday, buy a gift before", null);

        // then
        assertThat(result.types()).containsExactly(LunaElementTypeDto.APPOINTMENT, LunaElementTypeDto.TASK);
        assertThat(promptText()).contains("Dentist on Tuesday, buy a gift before").doesNotContain("The user marked");
    }

    @Test
    void thatClassifyingATextWithTypeHintShouldResultIntoAPromptWithTheHint() {
        // given
        answer("{\"types\": [\"INVOICE\"]}");

        // when
        var result = elementClassificationClient.classify("Electricity bill", LunaElementTypeDto.INVOICE);

        // then
        assertThat(result.types()).containsExactly(LunaElementTypeDto.INVOICE);
        assertThat(promptText()).contains("The user marked this input as INVOICE");
    }

    @Test
    void thatAnEmptyAnswerShouldResultIntoNoResult() {
        // given
        answer("null");

        // when
        var result = elementClassificationClient.classify("Something", null);

        // then
        assertThat(result).isNull();
    }

    private static ChatModel chatModel() {
        var chatModel = mock(ChatModel.class);
        ChatModelTestConfig.stubOptions(chatModel);
        return chatModel;
    }

    private void answer(String json) {
        when(chatModel.call(any(Prompt.class)))
                .thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage(json)))));
    }

    private String promptText() {
        var captor = ArgumentCaptor.forClass(Prompt.class);
        verify(chatModel).call(captor.capture());
        return captor.getValue().getContents();
    }
}
