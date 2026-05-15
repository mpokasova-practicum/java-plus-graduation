package ru.practicum.dto.comment;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import ru.practicum.enums.CommentStatus;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
@ToString
public class CommentDto {
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private long id;
    private String text;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private long eventId;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private long userId;

    private String created;

    private CommentStatus status;
}
