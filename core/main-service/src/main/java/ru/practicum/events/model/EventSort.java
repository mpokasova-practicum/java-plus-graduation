package ru.practicum.events.model;

public enum EventSort {
    EVENT_DATE,
    VIEWS;

    public static EventSort from(String sort) {
        return switch (sort.toLowerCase()) {
            case "event_date", "event date" -> EVENT_DATE;
            default -> VIEWS;
        };
    }
}
