package interswitch.academy.verve_guard.exceptions.advice;

import interswitch.academy.verve_guard.exceptions.BadRequestException;
import interswitch.academy.verve_guard.exceptions.ConflictException;
import interswitch.academy.verve_guard.exceptions.NotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.time.LocalDateTime.now;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

@Slf4j
@RestControllerAdvice
public class ResponseBodyAdviceImpl  {

    @ExceptionHandler({
            BadRequestException.class,
            IllegalArgumentException.class,
            IllegalStateException.class,
            MaxUploadSizeExceededException.class,
            ConversionFailedException.class,
            HttpRequestMethodNotSupportedException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<ApiError> handleBadRequestException(Exception e, HttpServletRequest request) {
        final var errorTraceId = UUID.randomUUID().toString();
        final var uri = request.getRequestURI();
        log.error("An error has occurred at {} with id {}", uri, errorTraceId);
        log.error(e.getMessage(), e);
        return ExceptionHandlerUtils.badRequest(e.getMessage(), uri, errorTraceId);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleHttpMessageNotReadableException(HttpMessageNotReadableException e, HttpServletRequest request) {
        final var errorTraceId = UUID.randomUUID().toString();
        final var uri = request.getRequestURI();
        log.error("Invalid request format error at {} with id {}", uri, errorTraceId);
        log.error(e.getMessage(), e);
        return ExceptionHandlerUtils.badRequest(ExceptionHandlerUtils.resolveMessage(e), uri, errorTraceId);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleMethodArgumentNotValidException(MethodArgumentNotValidException e, HttpServletRequest request) {
        final var message = ExceptionHandlerUtils.resolveValidationMessage(e);
        final var apiError = new ApiError(request.getRequestURI(), message, null, BAD_REQUEST.value(), now());
        return new ResponseEntity<>(apiError, BAD_REQUEST);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiError> handleConflictException(ConflictException e, HttpServletRequest request) {
        final var errorTraceId = UUID.randomUUID().toString();
        final var uri = request.getRequestURI();
        log.error("Conflict error at {} with id {}", uri, errorTraceId);
        log.error(e.getMessage(), e);
        return ExceptionHandlerUtils.conflict(e.getMessage(), uri, errorTraceId);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiError> handleHandlerMethodValidationException(HandlerMethodValidationException e, HttpServletRequest request) {
        final var message = ExceptionHandlerUtils.resolveHandlerValidationMessage(e);
        final var apiError = new ApiError(request.getRequestURI(), message, null, BAD_REQUEST.value(), now());
        return new ResponseEntity<>(apiError, BAD_REQUEST);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolationException(ConstraintViolationException e, HttpServletRequest request) {
        final var errorTraceId = UUID.randomUUID().toString();
        final var uri = request.getRequestURI();
        final var message = e.getConstraintViolations().stream()
                .map(violation -> {
                    String field = violation.getPropertyPath().toString();
                    field = field.contains(".") ? field.substring(field.lastIndexOf('.') + 1) : field;
                    return field + " " + violation.getMessage();
                })
                .collect(Collectors.joining(", "));
        log.error("Constraint violation at {} with id {}", uri, errorTraceId);
        return ExceptionHandlerUtils.badRequest(message, uri, errorTraceId);
    }

    @ExceptionHandler({
            NoSuchElementException.class,
            NotFoundException.class
    })
    public ResponseEntity<ApiError> handleNotFoundException(Exception e, HttpServletRequest request) {
        final var errorTraceId = UUID.randomUUID().toString();
        final var uri = request.getRequestURI();
        log.error("An error has occurred at {} with id {}", uri, errorTraceId);
        log.error(e.getMessage(), e);
        return ExceptionHandlerUtils.notFound(e.getMessage(), uri, errorTraceId);
    }

    @ExceptionHandler
    public ResponseEntity<ApiError> handleException(Exception e, HttpServletRequest request) {
        final var requestURI = request.getRequestURI();
        final var errorTraceId = UUID.randomUUID().toString();
        log.error("An internal server error has occurred at {} with id {}", requestURI, errorTraceId);
        log.error(ExceptionUtils.getStackTrace(e));
        final var apiError = new ApiError(requestURI, "An internal server error has occurred", errorTraceId, INTERNAL_SERVER_ERROR.value(), now());
        return new ResponseEntity<>(apiError, INTERNAL_SERVER_ERROR);
    }
}