CREATE TABLE calendars (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL,
    name VARCHAR(255),
    sport_type VARCHAR(255),
    season_year INTEGER,
    goal VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_calendars_owner_id ON calendars(owner_id);

CREATE TABLE events (
    id UUID PRIMARY KEY,
    calendar_id UUID,
    title VARCHAR(255) NOT NULL,
    start_ts TIMESTAMP,
    end_ts TIMESTAMP,
    timezone VARCHAR(255) NOT NULL,
    location VARCHAR(255),
    distance VARCHAR(255),
    priority VARCHAR(255),
    status VARCHAR(255),
    source VARCHAR(255),
    external_url VARCHAR(255),
    notes VARCHAR(2000),
    created_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_events_calendar_start ON events(calendar_id, start_ts);
