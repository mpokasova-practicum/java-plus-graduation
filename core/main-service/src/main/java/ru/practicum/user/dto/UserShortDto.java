package ru.practicum.user.dto;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
@ToString
public class UserShortDto {
    private Long id;
    private String name;
}
