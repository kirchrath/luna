package de.kirchrath.luna.mail.services;

import java.util.Properties;

import org.springframework.stereotype.Component;

import de.kirchrath.luna.mail.config.MailProperties;
import jakarta.mail.Session;

@Component
public class MailSessionProvider {

    private final MailProperties mailProperties;

    public MailSessionProvider(MailProperties mailProperties) {
        this.mailProperties = mailProperties;
    }

    public Session session() {
        var properties = new Properties();
        properties.putAll(mailProperties.session());
        return Session.getInstance(properties);
    }
}
