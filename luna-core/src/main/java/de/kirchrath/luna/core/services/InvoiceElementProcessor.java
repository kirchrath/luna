package de.kirchrath.luna.core.services;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import de.kirchrath.luna.core.converter.InvoiceElementConverter;
import de.kirchrath.luna.core.data.entity.LunaElementType;
import de.kirchrath.luna.core.data.entity.OutboundEvent;
import de.kirchrath.luna.core.data.entity.InvoiceElement;
import de.kirchrath.luna.core.data.repository.InvoiceElementRepository;
import de.kirchrath.luna.core.dto.InvoiceEventDataDto;
import de.kirchrath.luna.core.outbound.ElementEventPublisher;
import de.kirchrath.luna.core.outbound.ElementQualificationClient;
import jakarta.validation.Validator;

@Component
@Transactional(propagation = Propagation.MANDATORY)
public class InvoiceElementProcessor extends ElementProcessor<InvoiceEventDataDto, InvoiceElement> {

    private final InvoiceElementRepository invoiceElementRepository;
    private final InvoiceElementConverter invoiceElementConverter;

    public InvoiceElementProcessor(
            @Value("classpath:prompts/qualification/invoice.st") Resource prompt,
            ElementQualificationClient elementQualificationClient,
            ElementEventPublisher elementEventPublisher,
            Validator validator,
            InvoiceElementRepository invoiceElementRepository,
            InvoiceElementConverter invoiceElementConverter) {
        super(LunaElementType.INVOICE, "luna_core_invoice", prompt, InvoiceEventDataDto.class,
                elementQualificationClient, elementEventPublisher, validator);
        this.invoiceElementRepository = invoiceElementRepository;
        this.invoiceElementConverter = invoiceElementConverter;
    }

    @Override
    protected InvoiceElement toEntity(UUID outboundEventId, InvoiceEventDataDto data) {
        return invoiceElementConverter.toEntity(outboundEventId, data);
    }

    @Override
    protected InvoiceElement save(InvoiceElement element) {
        return invoiceElementRepository.save(element);
    }

    @Override
    protected Object toEventDto(OutboundEvent outboundEvent, InvoiceElement element) {
        return invoiceElementConverter.toEventDto(outboundEvent, element);
    }
}
