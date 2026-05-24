package ru.practicum.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.stereotype.Component;
import ru.practicum.config.KafkaClient;
import ru.practicum.consumer.EventSimilarityConsumer;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.handler.EventSimilarityHandler;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class SimilarityProcessor {

    private final EventSimilarityConsumer consumer;
    private final EventSimilarityHandler handler;
    private final KafkaClient kafkaClient;
    private final Map<TopicPartition, OffsetAndMetadata> currentOffsets = new HashMap<>();

    public void start() {
        log.info("Запуск SimilarityProcessor");

        Runtime.getRuntime().addShutdownHook(new Thread(consumer::wakeup));

        try {
            consumer.subscribe();
            Consumer<Long, SpecificRecordBase> kafkaConsumer = consumer.getConsumer();

            int totalRecords = 0;

            while (true) {
                ConsumerRecords<Long, SpecificRecordBase> records =
                        consumer.poll(kafkaClient.getPollTimeout());

                for (ConsumerRecord<Long, SpecificRecordBase> record : records) {
                    process(record);
                    manageOffsets(record, ++totalRecords, kafkaConsumer);
                }

                if (!records.isEmpty()) {
                    consumer.commitAsync(getCurrentOffsets());
                    log.debug("Коммит оффсетов после {} записей", records.count());
                }
            }
        } catch (WakeupException e) {
            log.info("Получен сигнал завершения для SimilarityProcessor");
        } catch (Exception e) {
            log.error("Ошибка в SimilarityProcessor", e);
        } finally {
            commitSyncOnShutdown();
            consumer.close();
            log.info("SimilarityProcessor завершен");
        }
    }

    public void process(ConsumerRecord<Long, SpecificRecordBase> record) {
        if (record.value() instanceof EventSimilarityAvro similarity) {
            try {
                handler.handle(similarity);
                log.debug("Обработано сходство: eventA={}, eventB={}, score={}",
                        similarity.getEventA(), similarity.getEventB(), similarity.getScore());
            } catch (Exception e) {
                log.error("Ошибка обработки сходства мероприятий {} и {}: {}",
                        similarity.getEventA(), similarity.getEventB(), e.getMessage(), e);
                throw e;
            }
        } else {
            log.warn("Получен record неверного типа: {}", record.value() != null ?
                    record.value().getClass().getName() : "null");
        }
    }

    public void manageOffsets(ConsumerRecord<Long, SpecificRecordBase> record,
                              int count, Consumer<Long, SpecificRecordBase> kafkaConsumer) {
        currentOffsets.put(
                new TopicPartition(record.topic(), record.partition()),
                new OffsetAndMetadata(record.offset() + 1)
        );

        if (count % 10 == 0) {
            kafkaConsumer.commitAsync(currentOffsets, (offsets, exception) -> {
                if (exception != null) {
                    log.error("Ошибка промежуточного коммита оффсетов: {}", offsets, exception);
                } else {
                    log.debug("Промежуточный коммит оффсетов выполнен: {}", offsets);
                }
            });
        }
    }

    public void commitSyncOnShutdown() {
        if (currentOffsets.isEmpty()) {
            log.debug("Нет оффсетов для коммита");
            return;
        }

        try {
            consumer.commitSync(currentOffsets);
            log.info("Финальный коммит оффсетов выполнен: {}", currentOffsets);
        } catch (Exception e) {
            log.error("Ошибка при финальном коммите оффсетов", e);
        }
    }

    public Map<TopicPartition, OffsetAndMetadata> getCurrentOffsets() {
        return new HashMap<>(currentOffsets);
    }
}
