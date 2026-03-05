package com.yelshod.diagnosticserviceai.persistence.repository;

import com.yelshod.diagnosticserviceai.analytics.ErrorsPerMinuteRow;
import com.yelshod.diagnosticserviceai.analytics.TopClusterRow;
import com.yelshod.diagnosticserviceai.analytics.TopExceptionRow;
import com.yelshod.diagnosticserviceai.persistence.entity.IncidentEntity;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IncidentRepository extends JpaRepository<IncidentEntity, UUID> {

    @Query(value = """
        SELECT date_trunc('minute', event_time) AS bucket, COUNT(*) AS count
        FROM incidents
        WHERE event_time BETWEEN :from AND :to
          AND (:service IS NULL OR service = :service)
        GROUP BY bucket
        ORDER BY bucket
        """, nativeQuery = true)
    List<ErrorsPerMinuteRow> errorsPerMinute(@Param("from") Instant from,
                                             @Param("to") Instant to,
                                             @Param("service") String service);

    @Query(value = """
        SELECT COALESCE(exception_type, 'Unknown') AS exceptionType, COUNT(*) AS count
        FROM incidents
        WHERE event_time BETWEEN :from AND :to
          AND (:service IS NULL OR service = :service)
        GROUP BY exceptionType
        ORDER BY count DESC
        LIMIT 10
        """, nativeQuery = true)
    List<TopExceptionRow> topExceptionTypes(@Param("from") Instant from,
                                            @Param("to") Instant to,
                                            @Param("service") String service);

    @Query(value = """
        SELECT cluster_key AS clusterKey, COUNT(*) AS count
        FROM incidents
        WHERE event_time BETWEEN :from AND :to
          AND (:service IS NULL OR service = :service)
        GROUP BY clusterKey
        ORDER BY count DESC
        LIMIT 10
        """, nativeQuery = true)
    List<TopClusterRow> topClusters(@Param("from") Instant from,
                                    @Param("to") Instant to,
                                    @Param("service") String service);
}
