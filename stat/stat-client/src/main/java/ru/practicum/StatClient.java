package ru.practicum;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import ru.practicum.dto.EndpointHitDto;
import ru.practicum.dto.ViewStats;
import ru.practicum.exception.ClientError;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
public class StatClient {
    private final RestClient restClient;
    @Value("${stats-server.url:http://stats-server:9090}")
    private String statUrl;

    public StatClient() {
        restClient = RestClient.builder()
                .build();
    }

    public void hit(@RequestBody @Valid EndpointHitDto dto) {
        String url = UriComponentsBuilder
                .fromHttpUrl(statUrl)
                .path("/hit")
                .build()
                .toUriString();

        ResponseEntity<EndpointHitDto> result = restClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .body(dto)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                    throw new ClientError(
                            response.getBody().toString()
                    );
                })
                .onStatus(HttpStatusCode::is5xxServerError, (request, response) -> {
                    throw new ClientError(
                            response.getBody().toString()
                    );
                })
                .toEntity(EndpointHitDto.class);

        if (result.getStatusCode() == HttpStatus.CREATED) {
            log.info("Информация сохранена: {}", dto);
        } else {
            log.error("Произошла ошибка с записью данных");
        }
    }

    public ResponseEntity<List<ViewStats>> getStats(@RequestParam(name = "start") @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime start,
                                                    @RequestParam(name = "end") @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime end,
                                                    @RequestParam(name = "uris", required = false) List<String> uris,
                                                    @RequestParam(name = "unique", required = false) Boolean unique) {
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl(statUrl + "/stats")
                .queryParam("start", start)
                .queryParam("end", end);
        if (uris != null && !uris.isEmpty()) {
            builder.queryParam("uris", uris.toArray());
        }

        if (unique != null) {
            builder.queryParam("unique", unique);
        }

        List<ViewStats> result = restClient.get()
                .uri(builder.build().toUriString())
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                    throw new ClientError(
                            response.getBody().toString()
                    );
                })
                .onStatus(HttpStatusCode::is5xxServerError, (request, response) -> {
                    throw new ClientError(
                            response.getBody().toString()
                    );
                })
                .body(new ParameterizedTypeReference<>() {
                });
        return ResponseEntity.ok(result);
    }


}
