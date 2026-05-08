package ru.practicum.request.mapper;

import lombok.experimental.UtilityClass;
import ru.practicum.request.dto.ParticipationRequestDto;
import ru.practicum.request.model.Request;

@UtilityClass
public class RequestMapper {
    public ParticipationRequestDto toParticipationRequestDto(Request req) {
        return ParticipationRequestDto.builder()
                .id(req.getId())
                .event(req.getEvent().getId())
                .requester(req.getRequester().getId())
                .created(req.getCreatedOn())
                .status(req.getStatus())
                .build();
    }
}
