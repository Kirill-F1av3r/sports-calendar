CREATE TABLE import_jobs (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    calendar_id UUID NOT NULL,
    status VARCHAR(32) NOT NULL,
    source_file_name VARCHAR(255) NOT NULL,
    source_content_type VARCHAR(255),
    source_file_size BIGINT NOT NULL,
    source_object_key VARCHAR(1000) NOT NULL,
    error_message TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    started_at TIMESTAMP WITH TIME ZONE,
    finished_at TIMESTAMP WITH TIME ZONE,
    applied_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_import_jobs_user_id ON import_jobs(user_id);
CREATE INDEX idx_import_jobs_calendar_id ON import_jobs(calendar_id);

CREATE TABLE import_draft_events (
    id UUID PRIMARY KEY,
    job_id UUID NOT NULL REFERENCES import_jobs(id) ON DELETE CASCADE,
    title VARCHAR(255),
    start_date DATE,
    end_date DATE,
    competition_level VARCHAR(32),
    location VARCHAR(255),
    external_url VARCHAR(500),
    priority VARCHAR(32),
    valid BOOLEAN NOT NULL,
    source_reference VARCHAR(255),
    raw_text TEXT,
    created_event_id UUID,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_import_draft_events_job_id ON import_draft_events(job_id);

CREATE TABLE import_draft_event_disciplines (
    draft_event_id UUID NOT NULL REFERENCES import_draft_events(id) ON DELETE CASCADE,
    position INTEGER NOT NULL,
    discipline VARCHAR(100) NOT NULL,
    PRIMARY KEY (draft_event_id, position)
);

CREATE TABLE import_draft_event_errors (
    id UUID PRIMARY KEY,
    draft_event_id UUID NOT NULL REFERENCES import_draft_events(id) ON DELETE CASCADE,
    field_name VARCHAR(100),
    message TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);
