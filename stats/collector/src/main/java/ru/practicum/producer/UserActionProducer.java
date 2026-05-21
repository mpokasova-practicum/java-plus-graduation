package ru.practicum.producer;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.stereotype.Component;
import ru.practicum.config.KafkaClient;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserActionProducer {
    private final KafkaClient kafkaClient;
    private Producer<Long, SpecificRecordBase> producer;

    public void sendUserAction(UserActionAvro userAction) {
        if (userAction == null) {
            log.warn("Попытка отправить null UserAction");
            return;
        }

        Producer<Long, SpecificRecordBase> kafkaProducer = getProducer();
        if (kafkaProducer == null) {
            log.error("Producer недоступен");
            return;
        }

        ProducerRecord<Long, SpecificRecordBase> record = new ProducerRecord<>(
                kafkaClient.getTopicsProperties().getUserActions(),
                null,
                userAction.getTimestamp().toEpochMilli(),
                userAction.getUserId(),
                userAction
        );

        kafkaProducer.send(record, (metadata, exception) -> {
            if (exception != null) {
                log.error("Ошибка отправки действия userId={}: {}",
                        userAction.getUserId(), exception.getMessage(), exception);
            } else {
                log.debug("Действие отправлено: userId={}, topic={}, offset={}",
                        userAction.getUserId(), metadata.topic(), metadata.offset());
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