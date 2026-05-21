package ru.practicum.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.springframework.stereotype.Component;
import ru.practicum.config.KafkaClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserActionConsumer {
    private final KafkaClient kafkaClient;
    private Consumer<Long, SpecificRecordBase> consumer;

    public ConsumerRecords<Long, SpecificRecordBase> poll(Duration timeout) {
        return getConsumer().poll(timeout);
    }

    public void subscribe() {
        getConsumer().subscribe(List.of(kafkaClient.getTopicsProperties().getUserActions()));
        log.info("Подписка на топик действий: {}", kafkaClient.getTopicsProperties().getUserActions());
    }

    public void commitAsync(Map<TopicPartition, OffsetAndMetadata> offsets) {
        getConsumer().commitAsync(offsets, (offsetsMap, exception) -> {
            if (exception != null) {
                log.error("Ошибка асинхронного коммита оффсетов: {}", offsetsMap, exception);
            } else {
                log.debug("Асинхронный коммит оффсетов выполнен: {}", offsetsMap);
            }
        });
    }

    public void commitSync(Map<TopicPartition, OffsetAndMetadata> offsets) {
        try {
            getConsumer().commitSync(offsets);
            log.debug("Синхронный коммит оффсетов выполнен: {}", offsets);
        } catch (Exception e) {
            log.error("Ошибка синхронного коммита оффсетов", e);
            throw e;
        }
    }

    public void wakeup() {
        if (consumer != null) {
            log.info("Вызов wakeup для consumer действий");
            consumer.wakeup();
        }
    }

    public void close() {
        if (consumer != null) {
            try {
                consumer.wakeup();
                consumer.close(Duration.ofMillis(100));
                log.info("Consumer действий успешно закрыт");
            } catch (Exception e) {
                log.error("Ошибка при закрытии consumer действий", e);
            } finally {
                consumer = null;
            }
        }
    }

    public Consumer<Long, SpecificRecordBase> getConsumer() {
        if (consumer == null) {
            consumer = kafkaClient.getConsumerAction();
        }
        return consumer;
    }
}
