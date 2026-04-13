package com.yelshod.diagnosticserviceai.ai;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiDiagnosisController {

    private final AiDiagnosisService aiDiagnosisService;

    @PostMapping("/diagnose")
    public AiDiagnosisResponse diagnose(@Valid @RequestBody AiDiagnosisRequest request) {
        return aiDiagnosisService.diagnose(request);
    }

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

    @ExceptionHandler(AiDiagnosisProviderException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    ErrorResponse handleProvider(AiDiagnosisProviderException ex) {
        return new ErrorResponse(ex.getMessage());
    }

    record ErrorResponse(String message) {
    }
}
