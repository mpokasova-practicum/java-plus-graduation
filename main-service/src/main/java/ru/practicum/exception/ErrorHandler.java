package ru.practicum.exception;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class ErrorHandler {
    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleHttpMessageNotReadableException(final HttpMessageNotReadableException e) {
        String message;

        if (e.getMessage() != null && e.getMessage().contains("Required request body is missing")) {
            message = "Тело запроса отсутствует или пустое";
        } else if (e.getCause() instanceof JsonParseException) {
            message = "Некорректный JSON формат";
        } else if (e.getCause() instanceof InvalidFormatException) {
            message = "Некорректный формат данных";
        } else {
            message = "Не удалось прочитать тело запроса";
        }

        log.error("400 {}", message, e);
        return new ApiError(
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "Неправильно создан запрос",
                message,
                LocalDateTime.now()
        );
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleValidationExceptions(final MethodArgumentNotValidException e) {
        String errorMessage = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));

        log.error("400 {}", errorMessage, e);
        return new ApiError(
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "Неправильно создан запрос",
                errorMessage,
                LocalDateTime.now()
        );
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleMissingParams(final MissingServletRequestParameterException e) {
        String message = String.format("Отсутствует обязательный параметр: '%s'",
                e.getParameterName());
        log.error("400 {}", message, e);
        return new ApiError(
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "Неправильно создан запрос",
                message,
                LocalDateTime.now()
        );
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleTypeMismatch(final MethodArgumentTypeMismatchException e) {
        String requiredType = e.getRequiredType() != null
                ? e.getRequiredType().getSimpleName()
                : "неизвестный тип";

        String message = String.format("Параметр '%s' должен быть типа %s",
                e.getName(),
                requiredType);
        log.error("400 {}", message, e);
        return new ApiError(
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "Неправильно создан запрос",
                message,
                LocalDateTime.now()
        );
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiError handleException(final Throwable e) {
        log.error("500 {}", e.getMessage(), e);
        return new ApiError(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(), "Возникла ошибка на сервере", e.getMessage(), LocalDateTime.now());
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleValidate(final ValidationException e) {
        log.error("400 {}", e.getMessage(), e);
        return new ApiError(HttpStatus.BAD_REQUEST.getReasonPhrase(), "Неправильно создан запрос", e.getMessage(), LocalDateTime.now());
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiError handleNotFound(final NotFoundException e) {
        log.error("404 {}", e.getMessage(), e);
        return new ApiError(HttpStatus.NOT_FOUND.getReasonPhrase(), "Не найден объект", e.getMessage(), LocalDateTime.now());
    }

    @ExceptionHandler({
            ConflictException.class,
            EventConflictException.class,
            RequestConflictException.class
    })
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handleConflict(final RuntimeException e) {
        log.error("409 {}", e.getMessage(), e);
        return new ApiError(HttpStatus.CONFLICT.getReasonPhrase(), "Произошел конфликт данных", e.getMessage(), LocalDateTime.now());
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handleConstraintViolation(final DataIntegrityViolationException e) {
        log.error("409 {}", e.getMessage(), e);
        return new ApiError(HttpStatus.CONFLICT.getReasonPhrase(), "Произошел конфликт данных", e.getMessage(), LocalDateTime.now());
    }

    @ExceptionHandler(DuplicateDataException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handleDuplicate(final DuplicateDataException e) {
        log.warn("409 {}", e.getMessage(), e);  // Изменил на warn, т.к. это клиентская ошибка
        return new ApiError(
                HttpStatus.CONFLICT.getReasonPhrase(),
                "Присутствует дубликат в запросе",
                e.getMessage(),
                LocalDateTime.now()
        );
    }


}
