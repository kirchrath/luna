package de.kirchrath.luna.core.converter;

import java.util.UUID;

import org.springframework.stereotype.Component;

import de.kirchrath.luna.core.data.entity.InvoiceElement;
import de.kirchrath.luna.core.data.entity.OutboundEvent;
import de.kirchrath.luna.core.dto.InvoiceEventDataDto;
import de.kirchrath.luna.core.dto.InvoiceEventDto;

@Component
public class InvoiceElementConverter {

    private final InvoiceElementMapper invoiceElementMapper;

    public InvoiceElementConverter(InvoiceElementMapper invoiceElementMapper) {
        this.invoiceElementMapper = invoiceElementMapper;
    }

    public InvoiceElement toEntity(UUID outboundEventId, InvoiceEventDataDto data) {
        return invoiceElementMapper.toEntity(outboundEventId, data);
    }

    public InvoiceEventDto toEventDto(OutboundEvent outboundEvent, InvoiceElement element) {
        return invoiceElementMapper.toEventDto(outboundEvent, element);
    }
}
