package de.kirchrath.luna.core.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class ClassificationApplicationServiceTest {

    private static final UUID INBOUND_EVENT_ID = UUID.fromString("3f2b9c1d-7e6a-4b5c-9d8e-1a2b3c4d5e6f");

    private final ClassificationDomainService classificationDomainService = mock(ClassificationDomainService.class);
    private final ClassificationApplicationService classificationApplicationService =
            new ClassificationApplicationService(classificationDomainService);

    @Test
    void thatFindingEventsToClassifyShouldResultIntoTheIdsOfTheDomainService() {
        // given
        when(classificationDomainService.findInboundEventIdsToClassify()).thenReturn(List.of(INBOUND_EVENT_ID));

        // when
        var found = classificationApplicationService.findInboundEventIdsToClassify();

        // then
        assertThat(found).containsExactly(INBOUND_EVENT_ID);
    }

    @Test
    void thatClassifyingAnEventShouldResultIntoDelegatingToTheDomainService() {
        // given
        var inboundEventId = INBOUND_EVENT_ID;

        // when
        classificationApplicationService.classify(inboundEventId);

        // then
        verify(classificationDomainService).classify(inboundEventId);
    }
}
