package de.kirchrath.luna.mail.config;

import java.time.Duration;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import de.kirchrath.luna.mail.data.entity.LifeArea;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * @param pollInterval  delay between two polls of all accounts
 * @param maxTextLength longer texts are cut to this number of characters
 * @param session       properties of the Jakarta Mail session, e.g. timeouts
 * @param defaults      server settings of accounts that do not set their own
 * @param accounts      the IMAP accounts by their name; set from the secret {@code mail_accounts_v<n>} in production
 */
@Validated
@ConfigurationProperties("app.mail")
public record MailProperties(
        Duration pollInterval,
        int maxTextLength,
        Map<String, String> session,
        Server defaults,
        Map<String, @Valid Account> accounts) {

    public MailProperties {
        session = session == null ? Map.of() : Map.copyOf(session);
        accounts = accounts == null ? Map.of() : Map.copyOf(accounts);
    }

    /**
     * @param host   IMAP host, reached with TLS
     * @param port   IMAP port
     * @param folder folder whose new emails are fetched
     */
    public record Server(String host, Integer port, String folder) {
    }

    /**
     * @param username address used to sign in
     * @param password app password
     * @param host     IMAP host, the default host if empty
     * @param port     IMAP port, the default port if empty
     * @param folder   folder, the default folder if empty
     * @param area     life area of all emails of this account; required, so no email lands in an unintended area
     */
    public record Account(
            String username, String password, String host, Integer port, String folder, @NotNull LifeArea area) {
    }
}
