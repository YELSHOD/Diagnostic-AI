package com.yelshod.diagnosticserviceai.cluster;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yelshod.diagnosticserviceai.logs.ErrorEvent;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClusterServiceTest {

    @Mock
    private ClusterKeyFactory clusterKeyFactory;

    @Mock
    private ClusterLifecycleService clusterLifecycleService;

    @Mock
    private IncidentRecorder incidentRecorder;

    @Mock
    private DiagnosisTrigger diagnosisTrigger;

    @InjectMocks
    private ClusterService clusterService;

    @Test
    void recordsIncidentAndSkipsDiagnosisWhenClusterAlreadyExists() {
        ErrorEvent event = new ErrorEvent(
                "orders",
                Instant.parse("2026-04-09T10:00:00Z"),
                "trace-1",
                "IllegalStateException",
                "boom",
                List.of("OrdersService.placeOrder"),
                "stack",
                List.of("ctx"));

        when(clusterKeyFactory.build("IllegalStateException", "OrdersService.placeOrder", "boom"))
                .thenReturn("cluster-1");
        when(clusterLifecycleService.upsert("cluster-1", event))
                .thenReturn(new ClusterLifecycleResult("cluster-1", "orders", 2L, false));

        clusterService.processEvent(event);

        verify(incidentRecorder).record("cluster-1", event);
        verify(diagnosisTrigger, never()).diagnoseNewCluster(any(), any());
    }
}
