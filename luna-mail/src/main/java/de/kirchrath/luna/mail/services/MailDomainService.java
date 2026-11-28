package de.kirchrath.luna.mail.services;

import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.kirchrath.luna.mail.config.MailProperties;
import de.kirchrath.luna.mail.converter.EmailConverter;
import de.kirchrath.luna.mail.data.entity.Mailbox;
import de.kirchrath.luna.mail.data.entity.MailboxStatus;
import de.kirchrath.luna.mail.data.repository.EmailRepository;
import de.kirchrath.luna.mail.data.repository.MailboxRepository;
import de.kirchrath.luna.mail.exception.MailFetchException;
import de.kirchrath.luna.mail.outbound.ImapAccount;
import de.kirchrath.luna.mail.outbound.ImapMailClient;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional
public class MailDomainService {

    private final MailProperties mailProperties;
    private final MailboxRepository mailboxRepository;
    private final EmailRepository emailRepository;
    private final ImapMailClient imapMailClient;
    private final EmailConverter emailConverter;
    private final TimeProvider timeProvider;
    private final ApplicationEventPublisher eventPublisher;

    public MailDomainService(
            MailProperties mailProperties,
            MailboxRepository mailboxRepository,
            EmailRepository emailRepository,
            ImapMailClient imapMailClient,
            EmailConverter emailConverter,
            TimeProvider timeProvider,
            ApplicationEventPublisher eventPublisher) {
        this.mailProperties = mailProperties;
        this.mailboxRepository = mailboxRepository;
        this.emailRepository = emailRepository;
        this.imapMailClient = imapMailClient;
        this.emailConverter = emailConverter;
        this.timeProvider = timeProvider;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(readOnly = true)
    public List<String> findAccountNames() {
        return mailProperties.accounts().keySet().stream().sorted().toList();
    }

    /**
     * Fetches the emails that arrived since the last poll. The first poll of an account and a changed UIDVALIDITY
     * only remember the current state, so older emails are never fetched. Failures are kept on the mailbox.
     */
    public void poll(String accountName) {
        var account = account(accountName);
        var mailbox = mailboxRepository.findByAccount(accountName)
                .orElseGet(() -> mailboxRepository.save(Mailbox.builder()
                        .account(accountName)
                        .address(account.username())
                        .folder(account.folder())
                        .status(MailboxStatus.OK)
                        .build()));
        try {
            var result = imapMailClient.fetch(account, mailbox.uidValidity(), mailbox.lastUid());
            if (mailbox.uidValidity() != null && mailbox.uidValidity() != result.uidValidity()) {
                log.warn("UIDVALIDITY of account {} changed, continuing with new emails only", accountName);
            }
            for (var fetched : result.emails()) {
                var email = emailRepository.save(emailConverter.toEmail(
                        mailbox.id(), mailProperties.accounts().get(accountName).area(), result.uidValidity(), fetched));
                eventPublisher.publishEvent(new EmailSavedEvent(emailConverter.toEvent(email, mailbox)));
            }
            mailbox = mailbox.toBuilder()
                    .uidValidity(result.uidValidity())
                    .lastUid(result.lastUid())
                    .status(MailboxStatus.OK)
                    .failures(0)
                    .lastError(null)
                    .build();
            log.debug("Fetched {} new email(s) of account {}", result.emails().size(), accountName);
        } catch (MailFetchException exception) {
            log.warn("Polling account {} failed", accountName, exception);
            mailbox = mailbox.toBuilder()
                    .status(MailboxStatus.FAILED)
                    .failures(mailbox.failures() + 1)
                    .lastError(exception.getCause().getMessage())
                    .build();
        }
        mailboxRepository.save(mailbox.toBuilder()
                .address(account.username())
                .folder(account.folder())
                .lastPolledDate(timeProvider.now())
                .build());
    }

    private ImapAccount account(String name) {
        var account = mailProperties.accounts().get(name);
        var defaults = mailProperties.defaults();
        return new ImapAccount(
                name,
                account.username(),
                account.password(),
                account.host() == null ? defaults.host() : account.host(),
                account.port() == null ? defaults.port() : account.port(),
                account.folder() == null ? defaults.folder() : account.folder());
    }
}
