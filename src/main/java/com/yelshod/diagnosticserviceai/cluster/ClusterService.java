package com.yelshod.diagnosticserviceai.cluster;

import com.yelshod.diagnosticserviceai.logs.ErrorEvent;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ClusterService {

    private final ClusterKeyFactory clusterKeyFactory;
    private final ClusterLifecycleService clusterLifecycleService;
    private final IncidentRecorder incidentRecorder;
    private final DiagnosisTrigger diagnosisTrigger;

    @Transactional
    public ClusterResult processEvent(ErrorEvent event) {
        String topFrame = event.stackFrames().isEmpty() ? "no-frame" : event.stackFrames().getFirst();
        String clusterKey = clusterKeyFactory.build(event.exceptionType(), topFrame, event.message());
        ClusterLifecycleResult lifecycleResult = clusterLifecycleService.upsert(clusterKey, event);
        incidentRecorder.record(clusterKey, event);

        if (lifecycleResult.newCluster()) {
            diagnosisTrigger.diagnoseNewCluster(clusterKey, event);
        }
        return new ClusterResult(
                lifecycleResult.clusterKey(),
                lifecycleResult.service(),
                lifecycleResult.count(),
                lifecycleResult.newCluster());
    }
}
