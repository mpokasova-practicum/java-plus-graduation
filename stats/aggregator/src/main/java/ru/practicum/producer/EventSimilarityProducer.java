package ru.practicum.producer;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.stereotype.Component;
import ru.practicum.config.KafkaClient;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventSimilarityProducer {
    private final KafkaClient kafkaClient;
    private Producer<Long, SpecificRecordBase> producer;

    public void send(EventSimilarityAvro similarity) {
        long timestamp = similarity.getTimestamp().toEpochMilli();
        Long key = similarity.getEventA();

        ProducerRecord<Long, SpecificRecordBase> record = new ProducerRecord<>(
                kafkaClient.getTopicsProperties().getEventsSimilarity(),
                null,
                timestamp,
                key,
                similarity
        );

        getProducer().send(record, (metadata, exception) -> {
            if (exception != null) {
                log.error("Ошибка отправки сходства мероприятий {} и {}: {}",
                        similarity.getEventA(), similarity.getEventB(), exception.getMessage(), exception);
            } else {
                log.debug("Сходство отправлено: firstEvent={}, secondEvent={}, score={}, offset={}",
                        similarity.getEventA(), similarity.getEventB(),
                        similarity.getScore(), metadata.offset());
            }
        });
    }

    @PreDestroy
    public void close() {
        if (producer != null) {
            try {
                producer.flush();
                producer.close(Duration.ofSeconds(30));
                log.info("Producer успешно закрыт");
            } catch (Exception e) {
                log.error("Ошибка при закрытии producer", e);
            } finally {
                producer = null;
            }
        }
    }

    private Producer<Long, SpecificRecordBase> getProducer() {
        if (producer == null) {
            producer = kafkaClient.getProducer();
        }
        return producer;
    }
}
