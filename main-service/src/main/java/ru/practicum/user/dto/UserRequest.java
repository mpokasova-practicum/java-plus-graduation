package ru.practicum.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class UserRequest {
    private List<Long> ids;
    private Integer from;
    private Integer size;
}
