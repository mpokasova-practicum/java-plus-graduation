package ru.practicum.client;

import feign.FeignException;
import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.EndpointHitDto;
import ru.practicum.dto.ViewStats;

import java.time.LocalDateTime;
import java.util.List;

@FeignClient(name = "stats-server")
public interface StatsClient {

    @PostMapping("/hit")
    @ResponseStatus(HttpStatus.CREATED)
    void hit(@Valid @RequestBody EndpointHitDto hitDto) throws FeignException;

    @GetMapping("/stats")
    ResponseEntity<List<ViewStats>> getStats(@RequestParam(name = "start") @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime start,
                                             @RequestParam(name = "end") @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime end,
                                             @RequestParam(name = "uris", required = false) List<String> uris,
                                             @RequestParam(name = "unique", defaultValue = "false") Boolean unique) throws FeignException;
}
