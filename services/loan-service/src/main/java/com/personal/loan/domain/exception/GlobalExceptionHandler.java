package com.personal.loan.domain.exception;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.personal.loan.api.dtos.ApiResponse;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Create a <code>ResponseEntity</code> describing exception
     * 
     * @param errorCode code of the exception
     * @param body      optional additional information
     */
    private <T> ResponseEntity<ApiResponse<T>> response(ErrorCode errorCode, T body) {
        ApiResponse<T> response = new ApiResponse<>(
                errorCode.getCode(),
                errorCode.getErrorMessage(),
                body);
        return ResponseEntity.status(errorCode.getHttpStatus()).body(response);
    }

    /**
     * Handles custom exception
     * 
     * @param e custom <code>WebException</code>
     * @return <code>ApiResponse</code> that describe the exception
     */
    @ExceptionHandler(value = WebException.class)
    ResponseEntity<ApiResponse<Void>> handleWebException(WebException e) {

        log.info("Handling custom WebException: {}", e.getErrorCode().name());
        return response(e.getErrorCode(), null);
    }

    /**
     * Handles exceptions occuring in runtime that haven't been considered
     * 
     * @param e Unhandled exception
     * @return <code>ApiResponse</code> that describe the exception
     */
    @ExceptionHandler(value = RuntimeException.class)
    ResponseEntity<ApiResponse<Void>> handleRuntimeException(RuntimeException e) {

        log.error("Catched unhandled exception: {}, message: {}", e, e.getMessage());

        return response(ErrorCode.UNCATEGORIZED_EXCEPTION, null);
    }

    /**
     * Handles exceptions created by Spring failing to validate passed parameters
     * into an endpoint.
     * For example, an attribute of a parameter is null when it's marked with
     * <code>@NotNull</code>
     * 
     * @param e catched <code>MethodArgumentNotValidException</code>
     * @return <code>ApiResponse</code> INVALID_ARGUMENTS error code
     */
    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    ResponseEntity<ApiResponse<Map<String, String>>> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException e) {
        log.info("Handling validation failure exception");
        Map<String, String> error = new HashMap<>();

        e.getBindingResult().getFieldErrors()
                .forEach(fieldError -> error.put(fieldError.getField(), fieldError.getDefaultMessage()));

        return response(ErrorCode.INVALID_ARGUMENTS, error);
    }

    /**
     * Handling exceptions created by passed data not matching expected data types
     * 
     * @param e catched <code>MethodArgumentTypeMismatchException</code>
     * @return <code>ApiResponse</code> with additional info about the exception
     */
    @ExceptionHandler(value = MethodArgumentTypeMismatchException.class)
    ResponseEntity<ApiResponse<String>> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException e) {
        log.info("Handling method argument type mismatch exception");

        String receivedValueTypeName = (e.getValue() != null) ? e.getValue().getClass().getSimpleName() : "null";
        String expectedValueTypeName = (e.getRequiredType() != null) ? e.getRequiredType().getSimpleName() : "null";

        String message = String.format(
                "Invalid value for parameter %s. Received type: %s. Expected type: %s",
                e.getName(),
                receivedValueTypeName,
                expectedValueTypeName);

        return response(ErrorCode.INVALID_ARGUMENTS, message);
    }
}
