package de.kirchrath.luna.core.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
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

import de.kirchrath.luna.core.config.ChatModelTestConfig;
import de.kirchrath.luna.core.converter.LunaElementTypeConverter;
import de.kirchrath.luna.core.converter.LunaElementTypeMapperImpl;
import de.kirchrath.luna.core.data.entity.EventData;
import de.kirchrath.luna.core.data.entity.EventSource;
import de.kirchrath.luna.core.data.entity.InboundEvent;
import de.kirchrath.luna.core.data.entity.InboundEventStatus;
import de.kirchrath.luna.core.data.entity.LunaElementType;
import de.kirchrath.luna.core.dto.LunaElementTypeDto;
import de.kirchrath.luna.core.dto.ManualInputDataDto;
import tools.jackson.databind.json.JsonMapper;

class ClassificationApplicationServiceTest {

    private static final UUID INBOUND_EVENT_ID = UUID.fromString("3f2b9c1d-7e6a-4b5c-9d8e-1a2b3c4d5e6f");

    private final ChatModel chatModel = chatModel();
    private final ClassificationDomainService classificationDomainService = mock(ClassificationDomainService.class);
    private final JsonMapper jsonMapper = JsonMapper.builder().build();
    private final ClassificationApplicationService classificationApplicationService =
            new ClassificationApplicationService(
                    classificationDomainService,
                    ChatClient.builder(chatModel),
                    new ClassPathResource("prompts/classification.st"),
                    jsonMapper,
                    new LunaElementTypeConverter(new LunaElementTypeMapperImpl()));

    @Test
    void thatFindingEventsToClassifyShouldResultIntoTheEventsOfTheDomainService() {
        // given
        var inboundEvent = inboundEvent(new ManualInputDataDto("Something", null));
        when(classificationDomainService.findInboundEventsToClassify()).thenReturn(List.of(inboundEvent));

        // when
        var found = classificationApplicationService.findInboundEventsToClassify();

        // then
        assertThat(found).containsExactly(inboundEvent);
    }

    @Test
    void thatClassifyingAnInputWithoutTypeHintShouldResultIntoTheDeterminedTypes() {
        // given
        var inboundEvent = inboundEvent(new ManualInputDataDto("Dentist on Tuesday, buy a gift before", null));
        answer("{\"types\": [\"APPOINTMENT\", \"TASK\"]}");

        // when
        classificationApplicationService.classify(inboundEvent);

        // then
        var outcome = completedOutcome(inboundEvent);
        assertThat(outcome.failed()).isFalse();
        assertThat(outcome.types()).containsExactly(LunaElementType.APPOINTMENT, LunaElementType.TASK);
        assertThat(promptText()).contains("Dentist on Tuesday, buy a gift before").doesNotContain("The user marked");
    }

    @Test
    void thatClassifyingAnInputWithTypeHintShouldResultIntoTheHintedTypeAndFurtherDeterminedTypes() {
        // given
        var inboundEvent = inboundEvent(
                new ManualInputDataDto("Electricity bill, contract ends in May", LunaElementTypeDto.INVOICE));
        answer("{\"types\": [\"INVOICE\", \"CONTRACT\"]}");

        // when
        classificationApplicationService.classify(inboundEvent);

        // then
        assertThat(completedOutcome(inboundEvent).types())
                .containsExactly(LunaElementType.INVOICE, LunaElementType.CONTRACT);
        assertThat(promptText()).contains("The user marked this input as INVOICE");
    }

    @ParameterizedTest
    @EnumSource(LunaElementTypeDto.class)
    void thatClassifyingAnInputWithTypeHintShouldResultIntoTheMatchingEntityType(LunaElementTypeDto typeHint) {
        // given
        var inboundEvent = inboundEvent(new ManualInputDataDto("Something", typeHint));
        answer("{\"types\": []}");

        // when
        classificationApplicationService.classify(inboundEvent);

        // then
        assertThat(completedOutcome(inboundEvent).types()).containsExactly(LunaElementType.valueOf(typeHint.name()));
    }

    @Test
    void thatClassifyingWithAResultWithoutTypesShouldResultIntoNoTypes() {
        // given
        var inboundEvent = inboundEvent(new ManualInputDataDto("Something", null));
        answer("{}");

        // when
        classificationApplicationService.classify(inboundEvent);

        // then
        var outcome = completedOutcome(inboundEvent);
        assertThat(outcome.failed()).isFalse();
        assertThat(outcome.types()).isEmpty();
    }

    @Test
    void thatClassifyingWithAnEmptyResultShouldResultIntoNoTypes() {
        // given
        var inboundEvent = inboundEvent(new ManualInputDataDto("Something", null));
        answer("null");

        // when
        classificationApplicationService.classify(inboundEvent);

        // then
        var outcome = completedOutcome(inboundEvent);
        assertThat(outcome.failed()).isFalse();
        assertThat(outcome.types()).isEmpty();
    }

    @Test
    void thatAFailingProviderShouldResultIntoAFailedOutcome() {
        // given
        var inboundEvent = inboundEvent(new ManualInputDataDto("Something", null));
        var failure = new IllegalStateException("provider unavailable");
        when(chatModel.call(any(Prompt.class))).thenThrow(failure);

        // when
        classificationApplicationService.classify(inboundEvent);

        // then
        var outcome = completedOutcome(inboundEvent);
        assertThat(outcome.failed()).isTrue();
        assertThat(outcome.failure()).isSameAs(failure);
        assertThat(outcome.types()).isEmpty();
    }

    private static ChatModel chatModel() {
        var chatModel = mock(ChatModel.class);
        ChatModelTestConfig.stubOptions(chatModel);
        return chatModel;
    }

    private InboundEvent inboundEvent(ManualInputDataDto input) {
        return new InboundEvent(INBOUND_EVENT_ID, EventSource.MANUAL, new EventData(jsonMapper.writeValueAsString(input)),
                InboundEventStatus.RECEIVED, 0, null, null, 0L);
    }

    private void answer(String json) {
        when(chatModel.call(any(Prompt.class)))
                .thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage(json)))));
    }

    private ClassificationOutcome completedOutcome(InboundEvent inboundEvent) {
        var captor = ArgumentCaptor.forClass(ClassificationOutcome.class);
        verify(classificationDomainService).completeClassification(eq(inboundEvent), captor.capture());
        return captor.getValue();
    }

    private String promptText() {
        var captor = ArgumentCaptor.forClass(Prompt.class);
        verify(chatModel).call(captor.capture());
        return captor.getValue().getContents();
    }
}
