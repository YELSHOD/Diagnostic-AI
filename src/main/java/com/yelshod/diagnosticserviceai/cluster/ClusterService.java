package com.yelshod.diagnosticserviceai.cluster;

import com.yelshod.diagnosticserviceai.logs.ErrorEvent;
import jakarta.transaction.Transactional;
import java.util.UUID;
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
        return processEvent(event, null);
    }

    @Transactional
    public ClusterResult processEvent(ErrorEvent event, UUID projectId) {
        String topFrame = event.stackFrames().isEmpty() ? "no-frame" : event.stackFrames().getFirst();
        String clusterKey = clusterKeyFactory.build(event.exceptionType(), topFrame, event.message());
        if (projectId != null) {
            clusterKey = "project:" + projectId + "|" + clusterKey;
        }
        ClusterLifecycleResult lifecycleResult = clusterLifecycleService.upsert(clusterKey, event, projectId);
        incidentRecorder.record(clusterKey, event, projectId);

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
