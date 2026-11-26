package de.kirchrath.luna.note.config;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.UUIDSerializer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.retrytopic.RetryTopicComponentFactory;
import org.springframework.kafka.retrytopic.RetryTopicConfiguration;
import org.springframework.kafka.retrytopic.RetryTopicConfigurationBuilder;
import org.springframework.kafka.retrytopic.RetryTopicConfigurationSupport;
import org.springframework.kafka.retrytopic.RetryTopicNamesProviderFactory;
import org.springframework.kafka.retrytopic.SuffixingRetryTopicNamesProviderFactory.SuffixingRetryTopicNamesProvider;
import org.springframework.kafka.support.serializer.DelegatingByTypeSerializer;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;

/**
 * Failed events of luna-core are retried on a single retry topic with a fixed delay and then go to the dead letter
 * topic of luna-note. Topic names follow the Luna convention instead of Spring's suffixes.
 */
@Configuration
@EnableKafka
@EnableConfigurationProperties({TopicProperties.class, RetryProperties.class})
public class KafkaConfig extends RetryTopicConfigurationSupport {

    private final TopicProperties topicProperties;

    public KafkaConfig(TopicProperties topicProperties) {
        this.topicProperties = topicProperties;
    }

    @Override
    protected RetryTopicComponentFactory createComponentFactory() {
        return new RetryTopicComponentFactory() {
            @Override
            public RetryTopicNamesProviderFactory retryTopicNamesProviderFactory() {
                return properties -> new SuffixingRetryTopicNamesProvider(properties) {
                    @Override
                    public String getTopicName(String topic) {
                        if (properties.isMainEndpoint()) {
                            return topic;
                        }
                        return properties.isDltTopic() ? topicProperties.dlt() : topicProperties.coreNoteRetry();
                    }
                };
            }
        };
    }

    @Bean
    RetryTopicConfiguration coreNoteRetryTopic(ProducerFactory<?, ?> producerFactory, RetryProperties retryProperties) {
        return RetryTopicConfigurationBuilder.newInstance()
                .maxAttempts(retryProperties.retries() + 1)
                .fixedBackOff(retryProperties.delay().toMillis())
                .useSingleTopicForSameIntervals()
                .includeTopic(topicProperties.coreNote())
                .create(retryKafkaTemplate(producerFactory));
    }

    /** Events that could not even be deserialized are forwarded as raw bytes, all others as JSON. */
    private KafkaTemplate<Object, Object> retryKafkaTemplate(ProducerFactory<?, ?> producerFactory) {
        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(
                producerFactory.getConfigurationProperties(),
                rawOr(new UUIDSerializer()),
                rawOr(new JacksonJsonSerializer<>())));
    }

    private Serializer<Object> rawOr(Serializer<?> serializer) {
        Map<Class<?>, Serializer<?>> delegates = new LinkedHashMap<>();
        delegates.put(byte[].class, new ByteArraySerializer());
        delegates.put(Object.class, serializer);
        return new DelegatingByTypeSerializer(delegates, true);
    }
}
