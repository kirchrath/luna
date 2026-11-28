package de.kirchrath.luna.mail.converter;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import de.kirchrath.luna.mail.data.entity.Email;
import de.kirchrath.luna.mail.data.entity.Mailbox;
import de.kirchrath.luna.mail.dto.AuditDto;
import de.kirchrath.luna.mail.dto.EmailEventDataDto;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface EmailMapper {

    @Mapping(target = "account", source = "mailbox.account")
    @Mapping(target = "address", source = "mailbox.address")
    @Mapping(target = "area", source = "email.area")
    @Mapping(target = "from", source = "email.sender")
    @Mapping(target = "to", source = "email.recipients")
    @Mapping(target = "messageId", source = "email.messageId")
    @Mapping(target = "subject", source = "email.subject")
    @Mapping(target = "sentDate", source = "email.sentDate")
    @Mapping(target = "receivedDate", source = "email.receivedDate")
    @Mapping(target = "text", source = "email.text")
    @Mapping(target = "textTruncated", source = "email.textTruncated")
    @Mapping(target = "attachments", source = "email.attachments")
    EmailEventDataDto toEventData(Email email, Mailbox mailbox);

    AuditDto toAudit(Email email);
}
