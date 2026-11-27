package de.kirchrath.luna.core.services;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import de.kirchrath.luna.core.data.entity.EventData;
import de.kirchrath.luna.core.data.entity.EventSource;
import de.kirchrath.luna.core.data.entity.InboundEvent;
import de.kirchrath.luna.core.data.entity.InboundEventStatus;
import de.kirchrath.luna.core.data.entity.LifeArea;
import de.kirchrath.luna.core.dto.LifeAreaDto;
import de.kirchrath.luna.core.dto.LunaElementTypeDto;
import de.kirchrath.luna.core.dto.ManualInputDataDto;
import de.kirchrath.luna.core.dto.mail.EmailAttachmentDto;
import de.kirchrath.luna.core.dto.mail.EmailEventDataDto;
import tools.jackson.databind.json.JsonMapper;

class InboundEventReaderTest {

    private static final OffsetDateTime CREATED = OffsetDateTime.parse("2026-11-26T12:00:00+01:00");
    private static final OffsetDateTime RECEIVED = OffsetDateTime.parse("2026-11-25T08:30:00+01:00");

    private final JsonMapper jsonMapper = JsonMapper.builder().build();
    private final TimeProvider timeProvider = new TimeProvider() {
        @Override
        public OffsetDateTime atServerOffset(Instant instant) {
            return instant.atOffset(ZoneOffset.ofHours(1));
        }
    };
    private final InboundEventReader inboundEventReader = new InboundEventReader(jsonMapper, timeProvider);

    @Test
    void thatReadingAManualInboundEventShouldResultIntoItsInputRelativeToItsCreation() {
        // given
        var input = new ManualInputDataDto("Dentist tomorrow", LifeAreaDto.PERSONAL, LunaElementTypeDto.APPOINTMENT);
        var inboundEvent = inboundEvent(EventSource.MANUAL, input);

        // when
        var analysisInput = inboundEventReader.analysisInput(inboundEvent);

        // then
        assertThat(analysisInput).isEqualTo(new AnalysisInput("Dentist tomorrow", LunaElementTypeDto.APPOINTMENT, CREATED));
        assertThat(inboundEventReader.manualInput(inboundEvent)).isEqualTo(input);
        assertThat(inboundEventReader.email(inboundEvent)).isNull();
    }

    @Test
    void thatReadingAMailInboundEventShouldResultIntoItsHeaderAttachmentsAndTextRelativeToItsArrival() {
        // given
        var email = new EmailEventDataDto()
                .account("private")
                .address("luna@luna.test")
                .from("Power company <billing@power.test>")
                .to(List.of("luna@luna.test", "copy@luna.test"))
                .subject("Invoice 4711")
                .receivedDate(RECEIVED)
                .text("Please pay by Friday.")
                .textTruncated(false)
                .attachments(List.of(
                        new EmailAttachmentDto().name("invoice.pdf").contentType("application/pdf"),
                        new EmailAttachmentDto().contentType("text/csv")));
        var inboundEvent = inboundEvent(EventSource.MAIL, email);

        // when
        var analysisInput = inboundEventReader.analysisInput(inboundEvent);

        // then
        assertThat(analysisInput.text()).isEqualTo("""
                From: Power company <billing@power.test>
                To: luna@luna.test, copy@luna.test
                Subject: Invoice 4711
                Received: 2026-11-25T08:30+01:00
                Attachments: invoice.pdf (application/pdf), text/csv

                Please pay by Friday.""");
        assertThat(analysisInput.typeHint()).isNull();
        assertThat(analysisInput.referenceTime()).isEqualTo(RECEIVED);
        assertThat(inboundEventReader.manualInput(inboundEvent)).isNull();
        assertThat(inboundEventReader.email(inboundEvent).getSubject()).isEqualTo("Invoice 4711");
    }

    @Test
    void thatReadingAMailWithoutSenderSubjectAndAttachmentsShouldResultIntoTheRemainingHeader() {
        // given
        var email = new EmailEventDataDto()
                .account("private")
                .address("luna@luna.test")
                .to(List.of("luna@luna.test"))
                .receivedDate(RECEIVED)
                .text("Hello")
                .textTruncated(false)
                .attachments(List.of());

        // when
        var analysisInput = inboundEventReader.analysisInput(inboundEvent(EventSource.MAIL, email));

        // then
        assertThat(analysisInput.text()).isEqualTo("""
                To: luna@luna.test
                Received: 2026-11-25T08:30+01:00

                Hello""");
    }

    private InboundEvent inboundEvent(EventSource source, Object data) {
        return new InboundEvent(UUID.randomUUID(), source, null, LifeArea.PERSONAL, new EventData(jsonMapper.writeValueAsString(data)),
                InboundEventStatus.RECEIVED, 0, CREATED, CREATED, 0L);
    }
}
