package ru.practicum.config;

import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.LongDeserializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import ru.practicum.deserializer.EventSimilarityDeserializer;
import ru.practicum.deserializer.UserActionDeserializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    @Value("${kafka.bootstrap.servers}")
    private String bootstrapServers;

    @Value("${kafka.consumer.group.action}")
    private String groupAction;

    @Value("${kafka.consumer.group.similarity}")
    private String groupSimilarity;

    @Value("${kafka.consumer.poll.timeout:1000}")
    private long pollTimeout;

    @Autowired
    private KafkaTopicsProperties topicsProperties;

    @Bean
    @Scope("prototype")
    public KafkaClient kafkaClient() {
        return new KafkaClient() {

            private Consumer<Long, SpecificRecordBase> consumerAction;
            private Consumer<Long, SpecificRecordBase> consumerSimilarity;

            @Override
            public Consumer<Long, SpecificRecordBase> getConsumerAction() {
                if (consumerAction == null) {
                    consumerAction = createConsumerAction();
                }
                return consumerAction;
            }

            @Override
            public Consumer<Long, SpecificRecordBase> getConsumerSimilarity() {
                if (consumerSimilarity == null) {
                    consumerSimilarity = createConsumerSimilarity();
                }
                return consumerSimilarity;
            }

            @Override
            public Duration getPollTimeout() {
                return Duration.ofMillis(pollTimeout);
            }

            @Override
            public KafkaTopicsProperties getTopicsProperties() {
                return topicsProperties;
            }

            private Consumer<Long, SpecificRecordBase> createConsumerAction() {
                Map<String, Object> config = new HashMap<>();
                config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
                config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, LongDeserializer.class);
                config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, UserActionDeserializer.class);
                config.put(ConsumerConfig.GROUP_ID_CONFIG, groupAction);
                return new KafkaConsumer<>(config);
            }

            private Consumer<Long, SpecificRecordBase> createConsumerSimilarity() {
                Map<String, Object> config = new HashMap<>();
                config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
                config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, LongDeserializer.class);
                config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, EventSimilarityDeserializer.class);
                config.put(ConsumerConfig.GROUP_ID_CONFIG, groupSimilarity);
                return new KafkaConsumer<>(config);
            }
        };
    }
}
