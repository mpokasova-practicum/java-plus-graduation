package ru.practicum.exception;

import java.time.LocalDateTime;

public record ApiError(String status, String reason, String message, LocalDateTime timestamp) {
}
