package de.kirchrath.luna.core.outbound;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
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
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ClassPathResource;

import de.kirchrath.luna.core.config.ChatModelTestConfig;
import de.kirchrath.luna.core.data.entity.LifeArea;
import de.kirchrath.luna.core.dto.NoteEventDataDto;
import de.kirchrath.luna.core.dto.QualificationResultDto;

class ElementQualificationClientTest {

    private final ChatModel chatModel = chatModel();
    private final ChatModel personalChatModel = chatModel();
    private final ElementQualificationClient elementQualificationClient = new ElementQualificationClient(
            new AreaChatClients(Map.of(
                    LifeArea.PERSONAL, ChatClient.builder(personalChatModel).build(),
                    LifeArea.WORK, ChatClient.builder(chatModel).build())));

    @Test
    void thatQualifyingATextShouldResultIntoTheItemsOfTheModel() {
        // given
        when(chatModel.call(any(Prompt.class))).thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage(
                "{\"items\": [{\"title\": \"Minutes\", \"summary\": \"Budget approved\"}]}")))));
        var referenceTime = OffsetDateTime.parse("2026-11-24T10:00:00+01:00");

        // when
        var result = elementQualificationClient.qualify(LifeArea.WORK, new ClassPathResource("prompts/qualification/note.st"),
                "Minutes of the meeting: the budget was approved", referenceTime,
                new ParameterizedTypeReference<QualificationResultDto<NoteEventDataDto>>() {
                });

        // then
        assertThat(result.items()).containsExactly(new NoteEventDataDto("Minutes", "Budget approved"));
        var captor = ArgumentCaptor.forClass(Prompt.class);
        verify(chatModel).call(captor.capture());
        assertThat(captor.getValue().getContents())
                .contains("Minutes of the meeting: the budget was approved")
                .contains("2026-11-24T10:00+01:00");
        verifyNoInteractions(personalChatModel);
    }

    private static ChatModel chatModel() {
        var chatModel = mock(ChatModel.class);
        ChatModelTestConfig.stubOptions(chatModel);
        return chatModel;
    }
}
