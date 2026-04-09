package com.yelshod.diagnosticserviceai.cluster;

public record ClusterLifecycleResult(
        String clusterKey,
        String service,
        long count,
        boolean newCluster
) {
}
