package de.kirchrath.luna.core.services;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

import de.kirchrath.luna.core.data.entity.InboundEvent;

class ClassificationSchedulerTest {

    private final ClassificationApplicationService classificationApplicationService =
            mock(ClassificationApplicationService.class);
    private final ClassificationScheduler classificationScheduler =
            new ClassificationScheduler(classificationApplicationService);

    @Test
    void thatRunningTheSchedulerShouldResultIntoClassifyingEveryEventToClassify() {
        // given
        var first = mock(InboundEvent.class);
        var second = mock(InboundEvent.class);
        when(classificationApplicationService.findInboundEventsToClassify()).thenReturn(List.of(first, second));

        // when
        classificationScheduler.classifyReceivedEvents();

        // then
        verify(classificationApplicationService).classify(first);
        verify(classificationApplicationService).classify(second);
    }
}
