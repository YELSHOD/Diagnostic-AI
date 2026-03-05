CREATE TABLE clusters (
    cluster_key VARCHAR(512) PRIMARY KEY,
    service VARCHAR(255) NOT NULL,
    title VARCHAR(500),
    severity VARCHAR(50),
    first_seen TIMESTAMP NOT NULL,
    last_seen TIMESTAMP NOT NULL,
    count BIGINT NOT NULL
);

CREATE TABLE incidents (
    id UUID PRIMARY KEY,
    cluster_key VARCHAR(512) NOT NULL REFERENCES clusters(cluster_key),
    service VARCHAR(255) NOT NULL,
    event_time TIMESTAMP NOT NULL,
    trace_id VARCHAR(255),
    exception_type VARCHAR(500),
    message TEXT,
    top_frame TEXT,
    stacktrace TEXT,
    context TEXT
);

CREATE INDEX idx_incidents_event_time ON incidents(event_time);
CREATE INDEX idx_incidents_service_event_time ON incidents(service, event_time);
CREATE INDEX idx_incidents_exception_type ON incidents(exception_type);

CREATE TABLE ai_diagnosis (
    cluster_key VARCHAR(512) PRIMARY KEY REFERENCES clusters(cluster_key),
    model VARCHAR(255) NOT NULL,
    prompt_version VARCHAR(64) NOT NULL,
    diagnosis_json JSONB NOT NULL,
    created_at TIMESTAMP NOT NULL
);
