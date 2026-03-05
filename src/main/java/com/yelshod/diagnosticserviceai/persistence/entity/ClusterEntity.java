package com.yelshod.diagnosticserviceai.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "clusters")
public class ClusterEntity {

    @Id
    @Column(name = "cluster_key", nullable = false, length = 512)
    private String clusterKey;

    @Column(nullable = false)
    private String service;

    private String title;

    private String severity;

    @Column(name = "first_seen", nullable = false)
    private Instant firstSeen;

    @Column(name = "last_seen", nullable = false)
    private Instant lastSeen;

    @Column(nullable = false)
    private long count;
}
