package de.kirchrath.luna.mail.outbound;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import de.kirchrath.luna.mail.exception.MailFetchException;
import de.kirchrath.luna.mail.services.MailSessionProvider;
import jakarta.mail.Folder;
import jakarta.mail.MessagingException;
import jakarta.mail.UIDFolder;
import jakarta.mail.internet.MimeMessage;

/** Reads new emails of an IMAP folder by their UIDs; always over TLS and without changing the mailbox. */
@Component
public class ImapMailClient {

    private static final String PROTOCOL = "imaps";
    private static final long NO_UID = 0;

    private final MailSessionProvider mailSessionProvider;
    private final EmailParser emailParser;

    public ImapMailClient(MailSessionProvider mailSessionProvider, EmailParser emailParser) {
        this.mailSessionProvider = mailSessionProvider;
        this.emailParser = emailParser;
    }

    /**
     * @param knownUidValidity UIDVALIDITY of the last poll, {@code null} on the first poll
     * @param lastUid          highest UID known from the last poll
     */
    public FetchResult fetch(ImapAccount account, Long knownUidValidity, Long lastUid) {
        try {
            var store = mailSessionProvider.session().getStore(PROTOCOL);
            store.connect(account.host(), account.port(), account.username(), account.password());
            try {
                return read(store.getFolder(account.folder()), knownUidValidity, lastUid);
            } finally {
                store.close();
            }
        } catch (MessagingException | IOException exception) {
            throw new MailFetchException("Fetching emails of account " + account.name() + " failed", exception);
        }
    }

    private FetchResult read(Folder folder, Long knownUidValidity, Long lastUid)
            throws MessagingException, IOException {
        folder.open(Folder.READ_ONLY);
        try {
            var uidFolder = (UIDFolder) folder;
            var uidValidity = uidFolder.getUIDValidity();
            var count = folder.getMessageCount();
            var highestUid = count == 0 ? NO_UID : uidFolder.getUID(folder.getMessage(count));
            if (knownUidValidity == null || uidValidity != knownUidValidity || highestUid <= lastUid) {
                return new FetchResult(uidValidity, highestUid, List.of());
            }
            var emails = new ArrayList<FetchedEmail>();
            // An explicit range instead of "lastUid + 1:*", which would return the last message again.
            for (var message : uidFolder.getMessagesByUID(lastUid + 1, highestUid)) {
                emails.add(emailParser.parse(uidFolder.getUID(message), (MimeMessage) message));
            }
            return new FetchResult(uidValidity, highestUid, List.copyOf(emails));
        } finally {
            folder.close();
        }
    }
}
