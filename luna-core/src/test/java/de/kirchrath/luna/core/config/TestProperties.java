package de.kirchrath.luna.core.config;

import org.springframework.core.io.ClassPathResource;

public final class TestProperties {

    private TestProperties() {
    }

    public static TopicProperties topics() {
        return new TopicProperties("luna_core_task", "luna_core_alert", "luna_core_appointment", "luna_core_invoice",
                "luna_core_contract", "luna_core_note", "luna_mail_email", "luna_core_email_retry", "luna_core_dlt");
    }

    public static PromptProperties prompts() {
        return new PromptProperties(
                new ClassPathResource("prompts/classification.st"),
                new ClassPathResource("prompts/qualification/task.st"),
                new ClassPathResource("prompts/qualification/alert.st"),
                new ClassPathResource("prompts/qualification/appointment.st"),
                new ClassPathResource("prompts/qualification/invoice.st"),
                new ClassPathResource("prompts/qualification/contract.st"),
                new ClassPathResource("prompts/qualification/note.st"));
    }
}
