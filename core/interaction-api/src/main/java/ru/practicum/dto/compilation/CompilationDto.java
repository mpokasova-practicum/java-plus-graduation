package ru.practicum.dto.compilation;

import lombok.*;
import ru.practicum.dto.event.EventShortDto;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
@ToString
public class CompilationDto {
    private Long id;
    private List<EventShortDto> events;
    private Boolean pinned = false;
    private String title;
}
