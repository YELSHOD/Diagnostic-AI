package com.yelshod.diagnosticserviceai.ai;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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
}
