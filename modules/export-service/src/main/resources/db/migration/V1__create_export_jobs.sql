CREATE TABLE export_jobs (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    calendar_id UUID NOT NULL,
    provider VARCHAR(255) NOT NULL,
    status VARCHAR(255) NOT NULL,
    spreadsheet_id VARCHAR(255),
    spreadsheet_url VARCHAR(1000),
    error_message VARCHAR(2000),
    created_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_export_jobs_user_id ON export_jobs(user_id);
CREATE INDEX idx_export_jobs_calendar_id ON export_jobs(calendar_id);
