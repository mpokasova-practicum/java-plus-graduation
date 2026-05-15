package ru.practicum.dto.user;

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
