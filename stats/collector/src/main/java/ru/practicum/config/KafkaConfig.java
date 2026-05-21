package ru.practicum.config;

import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.LongSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import ru.practicum.serializer.AvroSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    @Value("${kafka.bootstrap.servers}")
    private String bootstrapServers;

    @Value("${kafka.producer.client-id:collector-producer}")
    private String clientId;

    @Value("${kafka.consumer.poll.timeout:1000}")
    private long pollTimeout;

    @Autowired
    private KafkaTopicsProperties topicsProperties;

    @Bean
    @Scope("prototype")
    public KafkaClient kafkaClient() {
        return new KafkaClient() {

            private Producer<Long, SpecificRecordBase> producer;

            @Override
            public Producer<Long, SpecificRecordBase> getProducer() {
                if (producer == null) {
                    producer = createProducer();
                }
                return producer;
            }

            @Override
            public Consumer<Long, SpecificRecordBase> getConsumer() {
                throw new UnsupportedOperationException("Consumer не поддерживается в Collector");
            }

            @Override
            public Duration getPollTimeout() {
                return Duration.ofMillis(pollTimeout);
            }

            @Override
            public KafkaTopicsProperties getTopicsProperties() {
                return topicsProperties;
            }

            private Producer<Long, SpecificRecordBase> createProducer() {
                Map<String, Object> config = new HashMap<>();
                config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
                config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, LongSerializer.class);
                config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, AvroSerializer.class);
                return new KafkaProducer<>(config);
            }
        };
    }
}
