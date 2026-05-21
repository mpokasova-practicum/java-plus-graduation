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
import ru.practicum.consumer.UserActionConsumer;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.producer.EventSimilarityProducer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class SimilarityProcessor {
    private final SimilarityCalculator calculator;
    private final EventSimilarityProducer producer;
    private final UserActionConsumer consumer;
    private final KafkaClient kafkaClient;
    private final Map<TopicPartition, OffsetAndMetadata> currentOffsets = new HashMap<>();

    public void start() {
        log.info("Запуск SimilarityProcessor");

        Runtime.getRuntime().addShutdownHook(new Thread(consumer::wakeup));

        try {
            consumer.subscribe();
            Consumer<Long, SpecificRecordBase> kafkaConsumer = consumer.getRawConsumer();

            int totalRecords = 0;

            while (true) {
                ConsumerRecords<Long, SpecificRecordBase> records =
                        consumer.poll(kafkaClient.getPollTimeout());

                for (ConsumerRecord<Long, SpecificRecordBase> record : records) {
                    if (record.value() instanceof UserActionAvro action) {
                        process(action);
                        manageOffsets(record, ++totalRecords, kafkaConsumer);
                    }
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

    public void process(UserActionAvro action) {
        List<EventSimilarityAvro> similarities = calculator.calculateSimilarity(action);
        for (EventSimilarityAvro similarity : similarities) {
            producer.send(similarity);
            log.debug("Отправлено сходство: {} и {}, score={}",
                    similarity.getEventA(), similarity.getEventB(), similarity.getScore());
        }
    }

    public void manageOffsets(ConsumerRecord<Long, SpecificRecordBase> record,
                              int count,
                              Consumer<Long, SpecificRecordBase> kafkaConsumer) {
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
