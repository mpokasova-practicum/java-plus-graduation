package ru.practicum.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class ErrorHandler {
    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiError handleNotFoundException(NotFoundException exception, HttpServletRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND, exception, request);
    }

    @ExceptionHandler(BadRequestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleBadRequestException(BadRequestException exception, HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, exception, request);
    }

    @ExceptionHandler(ForbiddenException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiError handleForbiddenException(ForbiddenException exception, HttpServletRequest request) {
        return buildResponse(HttpStatus.FORBIDDEN, exception, request);
    }

    @ExceptionHandler(ConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handleConflictException(ConflictException exception, HttpServletRequest request) {
        return buildResponse(HttpStatus.CONFLICT, exception, request);
    }

    @ExceptionHandler(ValidationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handleValidationException(ValidationException exception, HttpServletRequest request) {
        return buildResponse(HttpStatus.CONFLICT, exception, request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleConstraintViolation(
            ConstraintViolationException ex, HttpServletRequest request) {
        String violations = ex.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .collect(Collectors.joining("; "));

        String message = violations.isEmpty() ? "Constraint violation" : violations;
        return buildResponse(HttpStatus.BAD_REQUEST, message, null, request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        String validationErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));

        String message = validationErrors.isEmpty() ? "Validation failed" : validationErrors;

        return buildResponse(HttpStatus.BAD_REQUEST, message, null, request);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleHandlerMethodValidation(
            HandlerMethodValidationException ex, HttpServletRequest request) {
        String validationErrors = ex.getAllErrors().stream()
                .map(MessageSourceResolvable::getDefaultMessage)
                .collect(Collectors.joining("; "));

        String message = validationErrors.isEmpty() ? "Validation failed" : validationErrors;

        return buildResponse(HttpStatus.BAD_REQUEST, message, null, request);
    }

    @ExceptionHandler(ServletRequestBindingException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleServletRequestBindingException(
            ServletRequestBindingException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), null, request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public ApiError handleHttpRequestMethodNotSupported(
            HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.METHOD_NOT_ALLOWED, ex.getMessage(), null, request);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    @ResponseStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
    public ApiError handleHttpMediaTypeNotSupported(
            HttpMediaTypeNotSupportedException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.UNSUPPORTED_MEDIA_TYPE, ex.getMessage(), null, request);
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiError handleNoHandlerFoundException(
            NoHandlerFoundException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), null, request);
    }

    @ExceptionHandler(Throwable.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiError handleUnexpected(Throwable exception, HttpServletRequest request) {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, exception, request);
    }

    private ApiError buildResponse(
            HttpStatus httpStatus,Throwable exception, HttpServletRequest request) {
        return buildResponse(httpStatus, exception.getMessage(), exception, request);
    }

    private ApiError buildResponse(
            HttpStatus httpStatus, String message, Throwable exception, HttpServletRequest request) {
        String path = request.getRequestURI();
        String httpMethod = request.getMethod();
        int statusCode = httpStatus.value();
        String error = httpStatus.getReasonPhrase();

        String logMessage = String.format("Path: %s %s, '%d' %s — %s", httpMethod, path, statusCode, error, message);

        if (statusCode >= 500) {
            log.error(logMessage, exception);
        } else {
            log.warn(logMessage);
        }

        return new ApiError(path, httpMethod, statusCode, error, message);
    }


}
