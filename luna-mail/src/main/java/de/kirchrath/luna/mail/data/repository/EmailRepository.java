package de.kirchrath.luna.mail.data.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.repository.CrudRepository;

import de.kirchrath.luna.mail.data.entity.Email;

public interface EmailRepository extends CrudRepository<Email, UUID> {

    List<Email> findByMailboxId(UUID mailboxId);
}
