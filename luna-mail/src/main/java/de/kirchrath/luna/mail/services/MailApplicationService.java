package de.kirchrath.luna.mail.services;

import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class MailApplicationService {

    private final MailDomainService mailDomainService;

    public MailApplicationService(MailDomainService mailDomainService) {
        this.mailDomainService = mailDomainService;
    }

    public List<String> findAccountNames() {
        return mailDomainService.findAccountNames();
    }

    public void poll(String accountName) {
        mailDomainService.poll(accountName);
    }
}
