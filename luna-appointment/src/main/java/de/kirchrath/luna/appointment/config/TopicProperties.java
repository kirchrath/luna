package de.kirchrath.luna.appointment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param coreAppointment      appointment events of luna-core (consumed)
 * @param coreAppointmentRetry retry topic for failed appointment events of luna-core
 * @param dlt           dead letter topic of luna-appointment
 * @param appointment          appointments of luna-appointment (published)
 */
@ConfigurationProperties("app.kafka.topics")
public record TopicProperties(String coreAppointment, String coreAppointmentRetry, String dlt, String appointment) {
}
