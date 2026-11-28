package de.kirchrath.luna.mail.converter;

import java.util.UUID;

import org.springframework.stereotype.Component;

import de.kirchrath.luna.mail.data.entity.Email;
import de.kirchrath.luna.mail.data.entity.EmailAttachment;
import de.kirchrath.luna.mail.data.entity.LifeArea;
import de.kirchrath.luna.mail.data.entity.Mailbox;
import de.kirchrath.luna.mail.dto.EmailEventDto;
import de.kirchrath.luna.mail.outbound.FetchedEmail;

@Component
public class EmailConverter {

    private final EmailMapper emailMapper;

    public EmailConverter(EmailMapper emailMapper) {
        this.emailMapper = emailMapper;
    }

    public Email toEmail(UUID mailboxId, LifeArea area, long uidValidity, FetchedEmail fetched) {
        return Email.builder()
                .mailboxId(mailboxId)
                .area(area)
                .uidValidity(uidValidity)
                .uid(fetched.uid())
                .messageId(fetched.messageId())
                .sender(fetched.sender())
                .recipients(fetched.recipients())
                .subject(fetched.subject())
                .sentDate(fetched.sentDate())
                .receivedDate(fetched.receivedDate())
                .text(fetched.text())
                .textTruncated(fetched.textTruncated())
                .attachments(fetched.attachments().stream()
                        .map(attachment -> new EmailAttachment(
                                attachment.name(), attachment.contentType(), attachment.size()))
                        .toList())
                .build();
    }

    public EmailEventDto toEvent(Email email, Mailbox mailbox) {
        return new EmailEventDto(email.id(), emailMapper.toAudit(email), emailMapper.toEventData(email, mailbox));
    }
}
