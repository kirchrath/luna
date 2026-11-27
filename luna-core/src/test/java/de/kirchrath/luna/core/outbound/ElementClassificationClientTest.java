package de.kirchrath.luna.core.outbound;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import de.kirchrath.luna.core.config.ChatModelTestConfig;
import de.kirchrath.luna.core.config.TestProperties;
import de.kirchrath.luna.core.data.entity.LifeArea;
import de.kirchrath.luna.core.dto.LunaElementTypeDto;

class ElementClassificationClientTest {

    private final ChatModel chatModel = chatModel();
    private final ChatModel workChatModel = chatModel();
    private final ElementClassificationClient elementClassificationClient = new ElementClassificationClient(
            new AreaChatClients(Map.of(
                    LifeArea.PERSONAL, ChatClient.builder(chatModel).build(),
                    LifeArea.WORK, ChatClient.builder(workChatModel).build())),
            TestProperties.prompts());

    @Test
    void thatClassifyingATextWithoutTypeHintShouldResultIntoTheTypesOfTheModel() {
        // given
        answer("{\"types\": [\"APPOINTMENT\", \"TASK\"]}");

        // when
        var result = elementClassificationClient.classify(LifeArea.PERSONAL, "Dentist on Tuesday, buy a gift before", null);

        // then
        assertThat(result.types()).containsExactly(LunaElementTypeDto.APPOINTMENT, LunaElementTypeDto.TASK);
        assertThat(promptText()).contains("Dentist on Tuesday, buy a gift before").doesNotContain("The user marked");
        verifyNoInteractions(workChatModel);
    }

    @Test
    void thatClassifyingATextWithTypeHintShouldResultIntoAPromptWithTheHint() {
        // given
        answer("{\"types\": [\"INVOICE\"]}");

        // when
        var result = elementClassificationClient.classify(LifeArea.PERSONAL, "Electricity bill", LunaElementTypeDto.INVOICE);

        // then
        assertThat(result.types()).containsExactly(LunaElementTypeDto.INVOICE);
        assertThat(promptText()).contains("The user marked this input as INVOICE");
    }

    @Test
    void thatAnEmptyAnswerShouldResultIntoNoResult() {
        // given
        answer("null");

        // when
        var result = elementClassificationClient.classify(LifeArea.PERSONAL, "Something", null);

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

    @Test
    void thatClassifyingAWorkInputShouldResultIntoARequestToTheProviderOfTheWorkArea() {
        // given
        when(workChatModel.call(any(Prompt.class)))
                .thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage("{\"types\": [\"TASK\"]}")))));

        // when
        var result = elementClassificationClient.classify(LifeArea.WORK, "Prepare the review", null);

        // then
        assertThat(result.types()).containsExactly(LunaElementTypeDto.TASK);
        verifyNoInteractions(chatModel);
    }
}
