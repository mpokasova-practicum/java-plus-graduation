package ru.practicum.config;

import feign.Retryer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class FeignRetryConfig {

    @Bean
    public Retryer retryer() {
        return new Retryer.Default(100, TimeUnit.SECONDS.toMillis(2), 3);
    }
}
