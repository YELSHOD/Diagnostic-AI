package com.yelshod.diagnosticserviceai.cluster;

import com.yelshod.diagnosticserviceai.ai.AiDiagnosisService;
import com.yelshod.diagnosticserviceai.config.AppProperties;
import com.yelshod.diagnosticserviceai.logs.ErrorEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class DiagnosisTrigger {

    private final AiDiagnosisService aiDiagnosisService;
    private final AppProperties appProperties;

    public void diagnoseNewCluster(String clusterKey, ErrorEvent event) {
        if (!appProperties.gemini().autoDiagnosisEnabled()) {
            log.debug("Auto AI diagnosis skipped clusterKey={} reason=disabled", clusterKey);
            return;
        }
        Thread.ofVirtual()
                .name("ai-diagnosis-trigger-", 0)
                .start(() -> {
                    try {
                        aiDiagnosisService.diagnoseNewCluster(clusterKey, event);
                    } catch (RuntimeException ex) {
                        log.warn("Async AI diagnosis trigger failed clusterKey={}", clusterKey, ex);
                    }
                });
    }
}
