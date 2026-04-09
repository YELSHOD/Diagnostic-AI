package com.yelshod.diagnosticserviceai.cluster;

import com.yelshod.diagnosticserviceai.ai.AiDiagnosisService;
import com.yelshod.diagnosticserviceai.logs.ErrorEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DiagnosisTrigger {

    private final AiDiagnosisService aiDiagnosisService;

    public void diagnoseNewCluster(String clusterKey, ErrorEvent event) {
        aiDiagnosisService.diagnoseNewCluster(clusterKey, event);
    }
}
