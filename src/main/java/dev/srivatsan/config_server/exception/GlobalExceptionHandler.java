package dev.srivatsan.config_server.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(ValidationException ex) {
        log.error("Validation error: {}", ex.getMessage());

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Validation Error")
                .message(ex.getMessage())
                .errorCode(ex.getErrorCode())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(NamespaceException.class)
    public ResponseEntity<ErrorResponse> handleNamespaceException(NamespaceException ex) {
        log.error("Namespace error: {}", ex.getMessage());

        HttpStatus status = switch (ex.getErrorCode()) {
            case NamespaceException.NAMESPACE_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case NamespaceException.NAMESPACE_ALREADY_EXISTS -> HttpStatus.CONFLICT;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error("Namespace Error")
                .message(ex.getMessage())
                .errorCode(ex.getErrorCode())
                .build();

        return ResponseEntity.status(status).body(error);
    }

    @ExceptionHandler(ConfigFileException.class)
    public ResponseEntity<ErrorResponse> handleConfigFileException(ConfigFileException ex) {
        log.error("Config file error: {}", ex.getMessage());

        HttpStatus status = switch (ex.getErrorCode()) {
            case ConfigFileException.CONFIG_FILE_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case ConfigFileException.CONFIG_FILE_ALREADY_EXISTS -> HttpStatus.CONFLICT;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error("Configuration File Error")
                .message(ex.getMessage())
                .errorCode(ex.getErrorCode())
                .build();

        return ResponseEntity.status(status).body(error);
    }

    @ExceptionHandler(GitOperationException.class)
    public ResponseEntity<ErrorResponse> handleGitOperationException(GitOperationException ex) {
        log.error("Git operation error: {}", ex.getMessage(), ex);

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Git Operation Error")
                .message(ex.getMessage())
                .errorCode(ex.getErrorCode())
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        log.error("Validation failed: {}", ex.getMessage());

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Validation Failed")
                .message("Request validation failed")
                .errorCode("VALIDATION_FAILED")
                .validationErrors(errors)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        log.error("Unexpected error occurred: {}", ex.getMessage(), ex);

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Internal Server Error")
                .message("An unexpected error occurred. Please try again later.")
                .errorCode("INTERNAL_ERROR")
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}