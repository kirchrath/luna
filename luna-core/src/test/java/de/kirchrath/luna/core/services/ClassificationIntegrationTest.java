package de.kirchrath.luna.core.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import de.kirchrath.luna.core.config.ChatModelTestConfig;
import de.kirchrath.luna.core.config.TestcontainersConfig;
import de.kirchrath.luna.core.data.entity.InboundEventStatus;
import de.kirchrath.luna.core.data.entity.LunaElementType;
import de.kirchrath.luna.core.data.entity.OutboundEvent;
import de.kirchrath.luna.core.data.entity.OutboundEventStatus;
import de.kirchrath.luna.core.data.repository.InboundEventRepository;
import de.kirchrath.luna.core.data.repository.OutboundEventRepository;
import de.kirchrath.luna.core.dto.ManualInputDataDto;

@SpringBootTest(properties = "app.classification.interval=1h")
@Import({TestcontainersConfig.class, ChatModelTestConfig.class})
class ClassificationIntegrationTest {

    @Autowired
    private InboundEventApplicationService inboundEventApplicationService;

    @Autowired
    private ClassificationScheduler classificationScheduler;

    @Autowired
    private InboundEventRepository inboundEventRepository;

    @Autowired
    private OutboundEventRepository outboundEventRepository;

    @Autowired
    private ChatModel chatModel;

    @BeforeEach
    void setUp() {
        outboundEventRepository.deleteAll();
        inboundEventRepository.deleteAll();
        reset(chatModel);
        ChatModelTestConfig.stubOptions(chatModel);
    }

    @Test
    void thatRunningTheClassificationSchedulerShouldResultIntoStoredOutboundEventsAndAnAnalyzedInboundEvent() {
        // given
        var reference = inboundEventApplicationService.createManualInboundEvent(
                new ManualInputDataDto("Dentist on Tuesday at 10, bring the bonus booklet", null));
        when(chatModel.call(any(Prompt.class))).thenReturn(new ChatResponse(
                List.of(new Generation(new AssistantMessage("{\"types\": [\"APPOINTMENT\", \"TASK\"]}")))));

        // when
        classificationScheduler.classifyReceivedEvents();

        // then
        var inboundEvent = inboundEventRepository.findById(reference.id()).orElseThrow();
        assertThat(inboundEvent.status()).isEqualTo(InboundEventStatus.ANALYZED);
        assertThat(inboundEvent.version()).isEqualTo(1L);
        var outboundEvents = outboundEventRepository.findByInboundEventId(reference.id());
        assertThat(outboundEvents)
                .extracting(OutboundEvent::type)
                .containsExactlyInAnyOrder(LunaElementType.APPOINTMENT, LunaElementType.TASK);
        assertThat(outboundEvents).allSatisfy(outboundEvent -> {
            assertThat(outboundEvent.id()).isNotNull();
            assertThat(outboundEvent.status()).isEqualTo(OutboundEventStatus.CLASSIFIED);
            assertThat(outboundEvent.createdDate()).isNotNull();
        });
    }

    @Test
    void thatRunningTheClassificationSchedulerWithAFailingProviderShouldResultIntoAnotherAttemptLater() {
        // given
        var reference = inboundEventApplicationService.createManualInboundEvent(new ManualInputDataDto("Something", null));
        when(chatModel.call(any(Prompt.class))).thenThrow(new IllegalStateException("provider unavailable"));

        // when
        classificationScheduler.classifyReceivedEvents();

        // then
        var inboundEvent = inboundEventRepository.findById(reference.id()).orElseThrow();
        assertThat(inboundEvent.status()).isEqualTo(InboundEventStatus.RECEIVED);
        assertThat(inboundEvent.attempts()).isEqualTo(1);
        assertThat(outboundEventRepository.findByInboundEventId(reference.id())).isEmpty();
    }
}
