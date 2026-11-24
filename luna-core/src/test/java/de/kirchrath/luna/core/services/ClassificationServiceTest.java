package de.kirchrath.luna.core.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.core.io.ClassPathResource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import de.kirchrath.luna.core.config.ChatModelTestConfig;
import de.kirchrath.luna.core.config.ClassificationProperties;
import de.kirchrath.luna.core.data.entity.EventData;
import de.kirchrath.luna.core.data.entity.EventSource;
import de.kirchrath.luna.core.data.entity.InboundEvent;
import de.kirchrath.luna.core.data.entity.InboundEventStatus;
import de.kirchrath.luna.core.data.entity.LunaElementType;
import de.kirchrath.luna.core.data.entity.OutboundEvent;
import de.kirchrath.luna.core.data.entity.OutboundEventStatus;
import de.kirchrath.luna.core.data.repository.InboundEventRepository;
import de.kirchrath.luna.core.data.repository.OutboundEventRepository;
import de.kirchrath.luna.core.dto.LunaElementTypeDto;
import de.kirchrath.luna.core.dto.ManualInputDataDto;
import tools.jackson.databind.json.JsonMapper;

class ClassificationServiceTest {

    private static final UUID INBOUND_EVENT_ID = UUID.fromString("3f2b9c1d-7e6a-4b5c-9d8e-1a2b3c4d5e6f");

    private final ChatModel chatModel = chatModel();
    private final InboundEventRepository inboundEventRepository = mock(InboundEventRepository.class);
    private final OutboundEventRepository outboundEventRepository = mock(OutboundEventRepository.class);
    private final JsonMapper jsonMapper = JsonMapper.builder().build();
    private final ClassificationService classificationService = new ClassificationService(
            ChatClient.builder(chatModel),
            new ClassPathResource("prompts/classification.st"),
            inboundEventRepository,
            outboundEventRepository,
            new TransactionTemplate(mock(PlatformTransactionManager.class)),
            jsonMapper,
            new ClassificationProperties(Duration.ofSeconds(30), 10, 3));

    @Test
    void thatClassifyingAnInputWithoutTypeHintShouldResultIntoOneOutboundEventPerDeterminedType() {
        // given
        var inboundEvent = inboundEvent(new ManualInputDataDto("Dentist on Tuesday, buy a gift before", null), 0);
        answer("{\"types\": [\"APPOINTMENT\", \"TASK\"]}");

        // when
        classificationService.classify(inboundEvent);

        // then
        assertThat(savedOutboundEventTypes(2)).containsExactly(LunaElementType.APPOINTMENT, LunaElementType.TASK);
        verify(inboundEventRepository).save(inboundEvent.withStatus(InboundEventStatus.ANALYZED));
        assertThat(promptText()).contains("Dentist on Tuesday, buy a gift before").doesNotContain("The user marked");
    }

    @Test
    void thatClassifyingAnInputWithTypeHintShouldResultIntoTheHintedTypeAndFurtherDeterminedTypes() {
        // given
        var inboundEvent = inboundEvent(new ManualInputDataDto("Electricity bill, contract ends in May", LunaElementTypeDto.INVOICE), 0);
        answer("{\"types\": [\"INVOICE\", \"CONTRACT\"]}");

        // when
        classificationService.classify(inboundEvent);

        // then
        assertThat(savedOutboundEventTypes(2)).containsExactly(LunaElementType.INVOICE, LunaElementType.CONTRACT);
        assertThat(promptText()).contains("The user marked this input as INVOICE");
    }

    @ParameterizedTest
    @EnumSource(LunaElementTypeDto.class)
    void thatClassifyingAnInputWithTypeHintShouldResultIntoTheMatchingEntityType(LunaElementTypeDto typeHint) {
        // given
        var inboundEvent = inboundEvent(new ManualInputDataDto("Something", typeHint), 0);
        answer("{\"types\": []}");

        // when
        classificationService.classify(inboundEvent);

        // then
        assertThat(savedOutboundEventTypes(1)).containsExactly(LunaElementType.valueOf(typeHint.name()));
    }

    @Test
    void thatClassifyingAnIrrelevantInputShouldResultIntoNoOutboundEventAndAnAnalyzedInboundEvent() {
        // given
        var inboundEvent = inboundEvent(new ManualInputDataDto("Great offers this week only", null), 0);
        answer("{\"types\": []}");

        // when
        classificationService.classify(inboundEvent);

        // then
        verify(outboundEventRepository, never()).save(any());
        verify(inboundEventRepository).save(inboundEvent.withStatus(InboundEventStatus.ANALYZED));
    }

    @Test
    void thatClassifyingWithAResultWithoutTypesShouldResultIntoNoOutboundEvent() {
        // given
        var inboundEvent = inboundEvent(new ManualInputDataDto("Something", null), 0);
        answer("{}");

        // when
        classificationService.classify(inboundEvent);

        // then
        verify(outboundEventRepository, never()).save(any());
        verify(inboundEventRepository).save(inboundEvent.withStatus(InboundEventStatus.ANALYZED));
    }

    @Test
    void thatClassifyingWithAnEmptyResultShouldResultIntoNoOutboundEvent() {
        // given
        var inboundEvent = inboundEvent(new ManualInputDataDto("Something", null), 0);
        answer("null");

        // when
        classificationService.classify(inboundEvent);

        // then
        verify(outboundEventRepository, never()).save(any());
        verify(inboundEventRepository).save(inboundEvent.withStatus(InboundEventStatus.ANALYZED));
    }

    @Test
    void thatAFailingClassificationShouldResultIntoAnIncreasedAttemptAndAReceivedInboundEvent() {
        // given
        var inboundEvent = inboundEvent(new ManualInputDataDto("Something", null), 0);
        when(chatModel.call(any(Prompt.class))).thenThrow(new IllegalStateException("provider unavailable"));

        // when
        classificationService.classify(inboundEvent);

        // then
        verify(outboundEventRepository, never()).save(any());
        verify(inboundEventRepository).save(inboundEvent.withAttempts(1).withStatus(InboundEventStatus.RECEIVED));
    }

    @Test
    void thatAFailingClassificationAtTheLastAttemptShouldResultIntoAFailedInboundEvent() {
        // given
        var inboundEvent = inboundEvent(new ManualInputDataDto("Something", null), 2);
        when(chatModel.call(any(Prompt.class))).thenThrow(new IllegalStateException("provider unavailable"));

        // when
        classificationService.classify(inboundEvent);

        // then
        verify(outboundEventRepository, never()).save(any());
        verify(inboundEventRepository).save(inboundEvent.withAttempts(3).withStatus(InboundEventStatus.FAILED));
    }

    private static ChatModel chatModel() {
        var chatModel = mock(ChatModel.class);
        ChatModelTestConfig.stubOptions(chatModel);
        return chatModel;
    }

    private InboundEvent inboundEvent(ManualInputDataDto input, int attempts) {
        return new InboundEvent(
                INBOUND_EVENT_ID,
                EventSource.MANUAL,
                new EventData(jsonMapper.writeValueAsString(input)),
                InboundEventStatus.RECEIVED,
                attempts,
                null,
                null,
                0L);
    }

    private void answer(String json) {
        when(chatModel.call(any(Prompt.class)))
                .thenReturn(new ChatResponse(java.util.List.of(new Generation(new AssistantMessage(json)))));
    }

    private java.util.List<LunaElementType> savedOutboundEventTypes(int count) {
        var captor = ArgumentCaptor.forClass(OutboundEvent.class);
        verify(outboundEventRepository, times(count)).save(captor.capture());
        captor.getAllValues().forEach(outboundEvent -> {
            assertThat(outboundEvent.id()).isNull();
            assertThat(outboundEvent.inboundEventId()).isEqualTo(INBOUND_EVENT_ID);
            assertThat(outboundEvent.status()).isEqualTo(OutboundEventStatus.CLASSIFIED);
        });
        return captor.getAllValues().stream().map(OutboundEvent::type).toList();
    }

    private String promptText() {
        var captor = ArgumentCaptor.forClass(Prompt.class);
        verify(chatModel).call(captor.capture());
        return captor.getValue().getContents();
    }
}
