package ru.practicum.exception;


public class ClientError extends RuntimeException {
    public ClientError(String message) {
        super(message);
    }
}
