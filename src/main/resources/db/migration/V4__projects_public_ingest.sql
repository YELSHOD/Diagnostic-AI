CREATE TABLE projects (
    id UUID PRIMARY KEY,
    name VARCHAR(160) NOT NULL,
    project_key VARCHAR(128) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL
);

ALTER TABLE runtime_targets
    ADD COLUMN project_id UUID REFERENCES projects(id);

ALTER TABLE clusters
    ADD COLUMN project_id UUID REFERENCES projects(id);

ALTER TABLE incidents
    ADD COLUMN project_id UUID REFERENCES projects(id);

CREATE TABLE log_events (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL REFERENCES projects(id),
    runtime_target_id UUID NOT NULL REFERENCES runtime_targets(id),
    service VARCHAR(255) NOT NULL,
    level VARCHAR(20) NOT NULL,
    message TEXT NOT NULL,
    stacktrace TEXT,
    environment VARCHAR(80),
    event_time TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_projects_project_key ON projects(project_key);
CREATE INDEX idx_runtime_targets_project_name ON runtime_targets(project_id, name);
CREATE INDEX idx_clusters_project_id ON clusters(project_id);
CREATE INDEX idx_incidents_project_event_time ON incidents(project_id, event_time);
CREATE INDEX idx_log_events_project_event_time ON log_events(project_id, event_time);
CREATE INDEX idx_log_events_runtime_target_event_time ON log_events(runtime_target_id, event_time);
