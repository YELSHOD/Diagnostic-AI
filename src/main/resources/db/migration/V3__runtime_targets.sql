CREATE TABLE runtime_targets (
    id UUID PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    type VARCHAR(40) NOT NULL,
    host VARCHAR(255),
    port INTEGER,
    health_url VARCHAR(500),
    log_source_type VARCHAR(40) NOT NULL,
    log_source_ref VARCHAR(1000),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_runtime_targets_type ON runtime_targets(type);
CREATE INDEX idx_runtime_targets_enabled ON runtime_targets(enabled);
