package ru.practicum.compilation.dto;

import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
@ToString
public class UpdateCompilationRequest {
    private List<Long> events;
    private Boolean pinned = false;

    @Size(min = 1, max = 50)
    private String title;
}
