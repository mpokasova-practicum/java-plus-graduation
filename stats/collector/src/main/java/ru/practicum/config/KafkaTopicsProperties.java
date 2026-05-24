package ru.practicum.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "spring.kafka.topics")
public class KafkaTopicsProperties {
    private String userActions = "stats.user-actions.v1";
}
