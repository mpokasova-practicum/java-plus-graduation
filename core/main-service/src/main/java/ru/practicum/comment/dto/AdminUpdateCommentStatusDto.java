package ru.practicum.comment.dto;

import lombok.*;
import ru.practicum.comment.model.AdminUpdateCommentStatusAction;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
@ToString
public class AdminUpdateCommentStatusDto {
    private AdminUpdateCommentStatusAction action;
}
