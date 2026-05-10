package ru.practicum;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients(basePackages = "ru.practicum.client")
@EnableDiscoveryClient
@ConfigurationPropertiesScan({"ru.practicum", "ru.practicum.client"})
public class MainService {

    public static void main(String[] args) {
        SpringApplication.run(MainService.class, args);
    }

}