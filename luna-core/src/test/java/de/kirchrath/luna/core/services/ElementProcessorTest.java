package de.kirchrath.luna.core.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.repository.CrudRepository;

import de.kirchrath.luna.core.config.TestProperties;
import de.kirchrath.luna.core.converter.AlertElementConverter;
import de.kirchrath.luna.core.converter.AlertElementMapperImpl;
import de.kirchrath.luna.core.converter.AppointmentElementConverter;
import de.kirchrath.luna.core.converter.AppointmentElementMapperImpl;
import de.kirchrath.luna.core.converter.ContractElementConverter;
import de.kirchrath.luna.core.converter.ContractElementMapperImpl;
import de.kirchrath.luna.core.converter.InvoiceElementConverter;
import de.kirchrath.luna.core.converter.InvoiceElementMapperImpl;
import de.kirchrath.luna.core.converter.NoteElementConverter;
import de.kirchrath.luna.core.converter.NoteElementMapperImpl;
import de.kirchrath.luna.core.converter.TaskElementConverter;
import de.kirchrath.luna.core.converter.TaskElementMapperImpl;
import de.kirchrath.luna.core.data.entity.EntityWithId;
import de.kirchrath.luna.core.data.entity.LunaElementType;
import de.kirchrath.luna.core.data.entity.OutboundEvent;
import de.kirchrath.luna.core.data.entity.OutboundEventStatus;
import de.kirchrath.luna.core.data.repository.AlertElementRepository;
import de.kirchrath.luna.core.data.repository.AppointmentElementRepository;
import de.kirchrath.luna.core.data.repository.ContractElementRepository;
import de.kirchrath.luna.core.data.repository.InvoiceElementRepository;
import de.kirchrath.luna.core.data.repository.NoteElementRepository;
import de.kirchrath.luna.core.data.repository.TaskElementRepository;
import de.kirchrath.luna.core.dto.AlertEventDataDto;
import de.kirchrath.luna.core.dto.AlertSeverityDto;
import de.kirchrath.luna.core.dto.AppointmentEventDataDto;
import de.kirchrath.luna.core.dto.AuditDto;
import de.kirchrath.luna.core.dto.ContractEventDataDto;
import de.kirchrath.luna.core.dto.ElementEventDto;
import de.kirchrath.luna.core.dto.InvoiceEventDataDto;
import de.kirchrath.luna.core.dto.NoteEventDataDto;
import de.kirchrath.luna.core.dto.QualificationResultDto;
import de.kirchrath.luna.core.dto.TaskEventDataDto;
import de.kirchrath.luna.core.dto.TaskPriorityDto;
import de.kirchrath.luna.core.outbound.ElementEventPublisher;
import de.kirchrath.luna.core.outbound.ElementQualificationClient;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

class ElementProcessorTest {

    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-11-24T10:00:00+01:00");
    private static final UUID OUTBOUND_EVENT_ID = UUID.fromString("6d1f0a3e-3c4b-4f63-8a3e-1e2f3a4b5c6d");
    private static final OutboundEvent OUTBOUND_EVENT = new OutboundEvent(
            OUTBOUND_EVENT_ID, UUID.fromString("3f2b9c1d-7e6a-4b5c-9d8e-1a2b3c4d5e6f"), LunaElementType.TASK,
            OutboundEventStatus.PUBLISHED, NOW, NOW, 0L);
    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    private final ElementQualificationClient elementQualificationClient = mock(ElementQualificationClient.class);
    private final ElementEventPublisher elementEventPublisher = mock(ElementEventPublisher.class);

    record Case<D>(
            LunaElementType type,
            String topic,
            String prompt,
            D validData,
            D invalidData,
            Function<Fixture, ElementProcessor<D, ?, ?>> factory) {

        @Override
        public String toString() {
            return type.name();
        }
    }

    record Fixture(
            ElementQualificationClient client, ElementEventPublisher publisher, CrudRepository<?, UUID> repository) {
    }

    static Stream<Case<?>> cases() {
        return Stream.of(
                new Case<>(LunaElementType.TASK, "luna_core_task", "prompts/qualification/task.st",
                        new TaskEventDataDto("Pay bill", "Electricity", NOW, TaskPriorityDto.HIGH),
                        new TaskEventDataDto(" ", null, null, null),
                        fixture -> new TaskElementProcessor(TestProperties.topics(), TestProperties.prompts(),
                                fixture.client(), fixture.publisher(), VALIDATOR, (TaskElementRepository) fixture.repository(),
                                new TaskElementConverter(new TaskElementMapperImpl()))),
                new Case<>(LunaElementType.ALERT, "luna_core_alert", "prompts/qualification/alert.st",
                        new AlertEventDataDto("Mail bounced", "Mailbox full", AlertSeverityDto.WARNING, "Mailserver"),
                        new AlertEventDataDto("Mail bounced", null, null, null),
                        fixture -> new AlertElementProcessor(TestProperties.topics(), TestProperties.prompts(),
                                fixture.client(), fixture.publisher(), VALIDATOR, (AlertElementRepository) fixture.repository(),
                                new AlertElementConverter(new AlertElementMapperImpl()))),
                new Case<>(LunaElementType.APPOINTMENT, "luna_core_appointment", "prompts/qualification/appointment.st",
                        new AppointmentEventDataDto("Dentist", NOW, NOW.plusHours(1), false, "Main street", null),
                        new AppointmentEventDataDto("Dentist", null, null, null, null, null),
                        fixture -> new AppointmentElementProcessor(TestProperties.topics(), TestProperties.prompts(),
                                fixture.client(), fixture.publisher(), VALIDATOR, (AppointmentElementRepository) fixture.repository(),
                                new AppointmentElementConverter(new AppointmentElementMapperImpl()))),
                new Case<>(LunaElementType.INVOICE, "luna_core_invoice", "prompts/qualification/invoice.st",
                        new InvoiceEventDataDto("Power Ltd", "R-1", new BigDecimal("42.50"), "EUR", NOW, null),
                        new InvoiceEventDataDto("Power Ltd", null, null, null, null, null),
                        fixture -> new InvoiceElementProcessor(TestProperties.topics(), TestProperties.prompts(),
                                fixture.client(), fixture.publisher(), VALIDATOR, (InvoiceElementRepository) fixture.repository(),
                                new InvoiceElementConverter(new InvoiceElementMapperImpl()))),
                new Case<>(LunaElementType.CONTRACT, "luna_core_contract", "prompts/qualification/contract.st",
                        new ContractEventDataDto("Power Ltd", "Electricity", NOW, null, NOW.plusMonths(1), null),
                        new ContractEventDataDto(null, "Electricity", null, null, null, null),
                        fixture -> new ContractElementProcessor(TestProperties.topics(), TestProperties.prompts(),
                                fixture.client(), fixture.publisher(), VALIDATOR, (ContractElementRepository) fixture.repository(),
                                new ContractElementConverter(new ContractElementMapperImpl()))),
                new Case<>(LunaElementType.NOTE, "luna_core_note", "prompts/qualification/note.st",
                        new NoteEventDataDto("Minutes", "Budget approved"),
                        new NoteEventDataDto("Minutes", ""),
                        fixture -> new NoteElementProcessor(TestProperties.topics(), TestProperties.prompts(),
                                fixture.client(), fixture.publisher(), VALIDATOR, (NoteElementRepository) fixture.repository(),
                                new NoteElementConverter(new NoteElementMapperImpl()))));
    }

    @ParameterizedTest
    @MethodSource("cases")
    <D> void thatQualifyingValidDataShouldResultIntoTheData(Case<D> testCase) {
        // given
        var processor = processor(testCase, mock(repositoryType(testCase.type())));
        when(elementQualificationClient.<D>qualify(any(), eq("Some input"), eq(NOW), any()))
                .thenReturn(new QualificationResultDto<>(List.of(testCase.validData(), testCase.validData())));

        // when
        var data = processor.qualify("Some input", NOW);

        // then
        assertThat(data).containsExactly(testCase.validData(), testCase.validData());
        assertThat(processor.type()).isEqualTo(testCase.type());
        verify(elementQualificationClient).qualify(eq(new ClassPathResource(testCase.prompt())), eq("Some input"),
                eq(NOW), any());
    }

    @ParameterizedTest
    @MethodSource("cases")
    <D> void thatQualifyingDataWithoutRequiredFieldsShouldResultIntoAConstraintViolation(Case<D> testCase) {
        // given
        var processor = processor(testCase, mock(repositoryType(testCase.type())));
        when(elementQualificationClient.<D>qualify(any(), any(), any(), any()))
                .thenReturn(new QualificationResultDto<>(List.of(testCase.validData(), testCase.invalidData())));

        // when / then
        assertThatThrownBy(() -> processor.qualify("Some input", NOW)).isInstanceOf(ConstraintViolationException.class);
    }

    @ParameterizedTest
    @MethodSource("cases")
    <D> void thatQualifyingWithoutResultShouldResultIntoAnError(Case<D> testCase) {
        // given
        var processor = processor(testCase, mock(repositoryType(testCase.type())));

        // when / then
        assertThatThrownBy(() -> processor.qualify("Some input", NOW))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(testCase.type().name());
    }

    @ParameterizedTest
    @MethodSource("cases")
    <D> void thatQualifyingWithoutItemsShouldResultIntoAnError(Case<D> testCase) {
        // given
        var processor = processor(testCase, mock(repositoryType(testCase.type())));
        when(elementQualificationClient.<D>qualify(any(), any(), any(), any()))
                .thenReturn(new QualificationResultDto<>(null));

        // when / then
        assertThatThrownBy(() -> processor.qualify("Some input", NOW)).isInstanceOf(IllegalStateException.class);
    }

    @ParameterizedTest
    @MethodSource("cases")
    <D> void thatQualifyingWithAnEmptyItemListShouldResultIntoAnError(Case<D> testCase) {
        // given
        var processor = processor(testCase, mock(repositoryType(testCase.type())));
        when(elementQualificationClient.<D>qualify(any(), any(), any(), any()))
                .thenReturn(new QualificationResultDto<>(List.of()));

        // when / then
        assertThatThrownBy(() -> processor.qualify("Some input", NOW)).isInstanceOf(IllegalStateException.class);
    }

    @ParameterizedTest
    @MethodSource("cases")
    @SuppressWarnings({"unchecked", "rawtypes"})
    <D> void thatStoringAndPublishingShouldResultIntoAStoredElementAndAnEventOnTheTopic(Case<D> testCase) {
        // given
        CrudRepository repository = mock(repositoryType(testCase.type()));
        when(repository.save(any())).thenAnswer(invocation -> ((EntityWithId<?>) invocation.getArgument(0))
                .withId(UUID.fromString("0b8f3a52-5a0e-4d7f-9f43-2f1c7f0f4a11")));
        var processor = processor(testCase, repository);

        // when
        processor.storeAndPublish(OUTBOUND_EVENT, testCase.validData());

        // then
        var captor = ArgumentCaptor.forClass(ElementEventDto.class);
        verify(elementEventPublisher).publish(eq(testCase.topic()), captor.capture());
        var event = (Record) captor.getValue();
        assertThat(event).hasFieldOrPropertyWithValue("id", OUTBOUND_EVENT_ID)
                .hasFieldOrPropertyWithValue("audit", new AuditDto(NOW, NOW, 0L))
                .hasFieldOrPropertyWithValue("data", testCase.validData());
    }

    private <D> ElementProcessor<D, ?, ?> processor(Case<D> testCase, CrudRepository<?, UUID> repository) {
        return testCase.factory().apply(new Fixture(elementQualificationClient, elementEventPublisher, repository));
    }

    private static Class<? extends CrudRepository> repositoryType(LunaElementType type) {
        return switch (type) {
            case TASK -> TaskElementRepository.class;
            case ALERT -> AlertElementRepository.class;
            case APPOINTMENT -> AppointmentElementRepository.class;
            case INVOICE -> InvoiceElementRepository.class;
            case CONTRACT -> ContractElementRepository.class;
            case NOTE -> NoteElementRepository.class;
        };
    }
}
