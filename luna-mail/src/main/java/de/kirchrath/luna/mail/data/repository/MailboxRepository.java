package de.kirchrath.luna.mail.data.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.repository.CrudRepository;

import de.kirchrath.luna.mail.data.entity.Mailbox;

public interface MailboxRepository extends CrudRepository<Mailbox, UUID> {

    Optional<Mailbox> findByAccount(String account);
}
