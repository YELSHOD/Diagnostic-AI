package com.yelshod.diagnosticserviceai.ai;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = {AiDiagnosisController.class, AiChatController.class})
public class AiApiExceptionHandler {

    @ExceptionHandler(AiDiagnosisDisabledException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    ErrorResponse handleDisabled(AiDiagnosisDisabledException ex) {
        return new ErrorResponse(ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ErrorResponse handleBadRequest(IllegalArgumentException ex) {
        return new ErrorResponse(ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ErrorResponse handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse("Invalid AI diagnosis request");
        return new ErrorResponse(message);
    }

    @ExceptionHandler(AiDiagnosisProviderException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    ErrorResponse handleProvider(AiDiagnosisProviderException ex) {
        return new ErrorResponse(ex.getMessage());
    }

    record ErrorResponse(String message) {
    }
}
