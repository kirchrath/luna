package de.kirchrath.luna.core.converter;

import java.util.List;

import org.springframework.stereotype.Component;

import de.kirchrath.luna.core.data.entity.InboundEvent;
import de.kirchrath.luna.core.data.entity.OutboundEvent;
import de.kirchrath.luna.core.dto.InboundEventListItemDto;
import de.kirchrath.luna.core.dto.ManualInputDataDto;
import de.kirchrath.luna.core.dto.mail.EmailEventDataDto;

@Component
public class InboundEventConverter {

    private final InboundEventMapper inboundEventMapper;

    public InboundEventConverter(InboundEventMapper inboundEventMapper) {
        this.inboundEventMapper = inboundEventMapper;
    }

    /**
     * @param manualInput the input of a manual inbound event, otherwise {@code null}
     * @param email       the email of a mail inbound event, otherwise {@code null}
     */
    public InboundEventListItemDto toListItemDto(
            InboundEvent inboundEvent,
            ManualInputDataDto manualInput,
            EmailEventDataDto email,
            List<OutboundEvent> outboundEvents) {
        return inboundEventMapper.toListItemDto(inboundEvent, manualInput, email, outboundEvents);
    }
}
