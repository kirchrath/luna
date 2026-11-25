package de.kirchrath.luna.core.converter;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import de.kirchrath.luna.core.data.entity.EventData;
import de.kirchrath.luna.core.data.entity.EventSource;
import de.kirchrath.luna.core.data.entity.InboundEvent;
import de.kirchrath.luna.core.data.entity.InboundEventStatus;
import de.kirchrath.luna.core.data.entity.LunaElementType;
import de.kirchrath.luna.core.data.entity.OutboundEvent;
import de.kirchrath.luna.core.data.entity.OutboundEventStatus;
import de.kirchrath.luna.core.dto.ElementReferenceDto;
import de.kirchrath.luna.core.dto.InboundEventStatusDto;
import de.kirchrath.luna.core.dto.LunaElementTypeDto;
import de.kirchrath.luna.core.dto.ManualInboundEventListItemDto;
import de.kirchrath.luna.core.dto.ManualInputDataDto;

class InboundEventConverterTest {

    private static final UUID INBOUND_ID = UUID.fromString("0b8f3a52-5a0e-4d7f-9f43-2f1c7f0f4a11");
    private static final UUID TASK_ID = UUID.fromString("6d1f0a3e-3c4b-4f63-8a3e-1e2f3a4b5c6d");
    private static final UUID APPOINTMENT_ID = UUID.fromString("9a4c2e1b-8d7f-4e3a-b2c1-0f9e8d7c6b5a");
    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-11-25T10:00:00+01:00");

    private final InboundEventConverter inboundEventConverter = new InboundEventConverter(new InboundEventMapperImpl());

    @Test
    void thatConvertingAnAnalyzedManualInboundEventShouldResultIntoAListItemWithItsElements() {
        // given
        var inboundEvent = new InboundEvent(INBOUND_ID, EventSource.MANUAL, new EventData("{}"),
                InboundEventStatus.ANALYZED, 1, NOW, NOW, 1L);
        var input = new ManualInputDataDto("Dentist on Tuesday, bring the bonus booklet", LunaElementTypeDto.APPOINTMENT);
        var outboundEvents = List.of(
                new OutboundEvent(APPOINTMENT_ID, INBOUND_ID, LunaElementType.APPOINTMENT, OutboundEventStatus.PUBLISHED, NOW, NOW, 0L),
                new OutboundEvent(TASK_ID, INBOUND_ID, LunaElementType.TASK, OutboundEventStatus.PUBLISHED, NOW, NOW, 0L));

        // when
        var item = inboundEventConverter.toManualListItemDto(inboundEvent, input, outboundEvents);

        // then
        assertThat(item).isEqualTo(new ManualInboundEventListItemDto(
                INBOUND_ID,
                "Dentist on Tuesday, bring the bonus booklet",
                LunaElementTypeDto.APPOINTMENT,
                InboundEventStatusDto.ANALYZED,
                NOW,
                List.of(new ElementReferenceDto(APPOINTMENT_ID, LunaElementTypeDto.APPOINTMENT),
                        new ElementReferenceDto(TASK_ID, LunaElementTypeDto.TASK))));
    }

    @Test
    void thatConvertingAReceivedManualInboundEventShouldResultIntoAListItemWithoutElements() {
        // given
        var inboundEvent = new InboundEvent(INBOUND_ID, EventSource.MANUAL, new EventData("{}"),
                InboundEventStatus.RECEIVED, 0, NOW, NOW, 0L);
        var input = new ManualInputDataDto("Milk is empty", null);

        // when
        var item = inboundEventConverter.toManualListItemDto(inboundEvent, input, List.of());

        // then
        assertThat(item.typeHint()).isNull();
        assertThat(item.status()).isEqualTo(InboundEventStatusDto.RECEIVED);
        assertThat(item.elements()).isEmpty();
    }
}
