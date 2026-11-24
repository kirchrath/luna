package de.kirchrath.luna.core.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

import de.kirchrath.luna.core.data.entity.InboundEvent;

class ClassificationApplicationServiceTest {

    private final ClassificationDomainService classificationDomainService = mock(ClassificationDomainService.class);
    private final ClassificationApplicationService classificationApplicationService =
            new ClassificationApplicationService(classificationDomainService);

    @Test
    void thatFindingEventsToClassifyShouldResultIntoTheEventsOfTheDomainService() {
        // given
        var inboundEvent = mock(InboundEvent.class);
        when(classificationDomainService.findInboundEventsToClassify()).thenReturn(List.of(inboundEvent));

        // when
        var found = classificationApplicationService.findInboundEventsToClassify();

        // then
        assertThat(found).containsExactly(inboundEvent);
    }

    @Test
    void thatClassifyingAnEventShouldResultIntoDelegatingToTheDomainService() {
        // given
        var inboundEvent = mock(InboundEvent.class);

        // when
        classificationApplicationService.classify(inboundEvent);

        // then
        verify(classificationDomainService).classify(inboundEvent);
    }
}
