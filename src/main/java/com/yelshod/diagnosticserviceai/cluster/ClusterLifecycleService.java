package com.yelshod.diagnosticserviceai.cluster;

import com.yelshod.diagnosticserviceai.logs.ErrorEvent;
import com.yelshod.diagnosticserviceai.persistence.entity.ClusterEntity;
import com.yelshod.diagnosticserviceai.persistence.repository.ClusterRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ClusterLifecycleService {

    private final ClusterRepository clusterRepository;

    public ClusterLifecycleResult upsert(String clusterKey, ErrorEvent event) {
        Instant now = Instant.now();
        ClusterEntity cluster = clusterRepository.findById(clusterKey).orElse(null);
        boolean isNew = cluster == null;

        if (isNew) {
            cluster = ClusterEntity.builder()
                    .clusterKey(clusterKey)
                    .service(event.service())
                    .title(event.exceptionType())
                    .severity("high")
                    .firstSeen(now)
                    .lastSeen(now)
                    .count(1)
                    .build();
        } else {
            cluster.setCount(cluster.getCount() + 1);
            cluster.setLastSeen(now);
        }

        clusterRepository.save(cluster);
        return new ClusterLifecycleResult(clusterKey, event.service(), cluster.getCount(), isNew);
    }
}
