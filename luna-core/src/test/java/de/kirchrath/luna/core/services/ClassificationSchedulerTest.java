package de.kirchrath.luna.core.services;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class ClassificationSchedulerTest {

    private final ClassificationApplicationService classificationApplicationService =
            mock(ClassificationApplicationService.class);
    private final ClassificationScheduler classificationScheduler =
            new ClassificationScheduler(classificationApplicationService);

    @Test
    void thatRunningTheSchedulerShouldResultIntoClassifyingEveryEventToClassify() {
        // given
        var first = UUID.fromString("3f2b9c1d-7e6a-4b5c-9d8e-1a2b3c4d5e6f");
        var second = UUID.fromString("0b8f3a52-5a0e-4d7f-9f43-2f1c7f0f4a11");
        when(classificationApplicationService.findInboundEventIdsToClassify()).thenReturn(List.of(first, second));

        // when
        classificationScheduler.classifyReceivedEvents();

        // then
        verify(classificationApplicationService).classify(first);
        verify(classificationApplicationService).classify(second);
    }
}
