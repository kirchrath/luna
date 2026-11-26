package de.kirchrath.luna.task.config;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.jdbc.core.convert.JdbcCustomConversions;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;

import de.kirchrath.luna.task.services.TimeProvider;

@Configuration
@EnableJdbcAuditing(dateTimeProviderRef = "auditingDateTimeProvider")
public class JdbcConfig {

    @Bean
    DateTimeProvider auditingDateTimeProvider(TimeProvider timeProvider) {
        return () -> Optional.of(timeProvider.now());
    }

    @Bean
    JdbcCustomConversions jdbcCustomConversions(TimeProvider timeProvider) {
        return new JdbcCustomConversions(List.of(new TimestampReadingConverter(timeProvider)));
    }

    @ReadingConverter
    static class TimestampReadingConverter implements Converter<Timestamp, OffsetDateTime> {

        private final TimeProvider timeProvider;

        TimestampReadingConverter(TimeProvider timeProvider) {
            this.timeProvider = timeProvider;
        }

        @Override
        public OffsetDateTime convert(Timestamp source) {
            return timeProvider.atServerOffset(source.toInstant());
        }
    }
}
