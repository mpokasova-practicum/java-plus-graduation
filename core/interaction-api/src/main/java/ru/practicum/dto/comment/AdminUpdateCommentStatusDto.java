package ru.practicum.dto.comment;

import lombok.*;
import ru.practicum.enums.AdminUpdateCommentStatusAction;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
@ToString
public class AdminUpdateCommentStatusDto {
    private AdminUpdateCommentStatusAction action;
}
