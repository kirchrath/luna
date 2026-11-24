package de.kirchrath.luna.core.config;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.postgresql.util.PGobject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.jdbc.core.convert.JdbcCustomConversions;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;

import de.kirchrath.luna.core.data.entity.EventData;
import de.kirchrath.luna.core.services.TimeProvider;

@Configuration
@EnableJdbcAuditing(dateTimeProviderRef = "auditingDateTimeProvider")
public class JdbcConfig {

    @Bean
    DateTimeProvider auditingDateTimeProvider(TimeProvider timeProvider) {
        return () -> Optional.of(timeProvider.now());
    }

    @Bean
    JdbcCustomConversions jdbcCustomConversions(TimeProvider timeProvider) {
        return new JdbcCustomConversions(List.of(
                new EventDataWritingConverter(),
                new EventDataReadingConverter(),
                new TimestampReadingConverter(timeProvider)));
    }

    @WritingConverter
    static class EventDataWritingConverter implements Converter<EventData, PGobject> {

        @Override
        public PGobject convert(EventData source) {
            var jsonb = new PGobject();
            jsonb.setType("jsonb");
            try {
                jsonb.setValue(source.json());
            } catch (SQLException e) {
                throw new IllegalStateException("Could not convert event data to jsonb", e);
            }
            return jsonb;
        }
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

    @ReadingConverter
    static class EventDataReadingConverter implements Converter<PGobject, EventData> {

        @Override
        public EventData convert(PGobject source) {
            return new EventData(source.getValue());
        }
    }
}
