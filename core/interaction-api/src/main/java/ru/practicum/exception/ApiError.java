package ru.practicum.exception;

public record ApiError(

        String path,

        String httpMethod,

        int statusCode,

        String error,

        String message
) {
}
