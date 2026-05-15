package ru.practicum.dto.event;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EventWithCountConfirmedRequests {

    private Long eventId;

    private int countConfirmedRequests;

    public EventWithCountConfirmedRequests(Long eventId, Long countConfirmedRequestsLong) {
        this.eventId = eventId;
        this.countConfirmedRequests = (countConfirmedRequestsLong == null) ? 0 : countConfirmedRequestsLong.intValue();
    }
}
