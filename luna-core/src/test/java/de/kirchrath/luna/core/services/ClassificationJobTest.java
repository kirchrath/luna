package de.kirchrath.luna.core.services;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;

import de.kirchrath.luna.core.config.ClassificationProperties;
import de.kirchrath.luna.core.data.entity.InboundEvent;
import de.kirchrath.luna.core.data.repository.InboundEventRepository;

class ClassificationJobTest {

    private final InboundEventRepository inboundEventRepository = mock(InboundEventRepository.class);
    private final ClassificationService classificationService = mock(ClassificationService.class);
    private final ClassificationJob classificationJob = new ClassificationJob(
            inboundEventRepository, classificationService, new ClassificationProperties(Duration.ofSeconds(30), 10, 3));

    @Test
    void thatRunningTheJobShouldResultIntoClassifyingTheOldestReceivedEventsOfOneBatch() {
        // given
        var first = mock(InboundEvent.class);
        var second = mock(InboundEvent.class);
        when(inboundEventRepository.findOldestByStatus("RECEIVED", 10)).thenReturn(List.of(first, second));

        // when
        classificationJob.classifyReceivedEvents();

        // then
        verify(classificationService).classify(first);
        verify(classificationService).classify(second);
    }
}
