package de.kirchrath.luna.core.data.entity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;

import lombok.With;

public record InvoiceElement(
        @With @Id UUID id,
        UUID outboundEventId,
        String issuer,
        String invoiceNumber,
        BigDecimal amount,
        String currency,
        OffsetDateTime dueDate,
        String description,
        @CreatedDate OffsetDateTime createdDate,
        @LastModifiedDate OffsetDateTime lastModifiedDate,
        @Version Long version) implements EntityWithId<InvoiceElement> {
}
