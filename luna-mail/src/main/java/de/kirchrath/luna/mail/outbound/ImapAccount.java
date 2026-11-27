package de.kirchrath.luna.mail.outbound;

/** A configured account with its effective server settings. */
public record ImapAccount(String name, String username, String password, String host, int port, String folder) {
}
