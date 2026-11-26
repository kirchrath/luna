package de.kirchrath.luna.mail.services;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class MailScheduler {

    private final MailApplicationService mailApplicationService;

    public MailScheduler(MailApplicationService mailApplicationService) {
        this.mailApplicationService = mailApplicationService;
    }

    @Scheduled(initialDelayString = "${app.mail.poll-interval}", fixedDelayString = "${app.mail.poll-interval}")
    public void pollAccounts() {
        mailApplicationService.findAccountNames().forEach(mailApplicationService::poll);
    }
}
