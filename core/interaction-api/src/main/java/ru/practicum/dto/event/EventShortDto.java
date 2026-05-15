package ru.practicum.dto.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import lombok.experimental.FieldDefaults;
import ru.practicum.constants.DateTimePattern;
import ru.practicum.dto.category.CategoryDto;
import ru.practicum.dto.user.UserShortDto;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EventShortDto {

        Long id;

        String annotation;

        CategoryDto category;

        Long confirmedRequests;

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = DateTimePattern.DATE_TIME_PATTERN)
        LocalDateTime eventDate;

        UserShortDto initiator;

        Boolean paid;

        String title;

        Long views;
}
