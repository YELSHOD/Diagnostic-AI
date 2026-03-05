package com.yelshod.diagnosticserviceai.cluster;

public record ClusterResult(
        String clusterKey,
        String service,
        long count,
        boolean newCluster
) {
}
