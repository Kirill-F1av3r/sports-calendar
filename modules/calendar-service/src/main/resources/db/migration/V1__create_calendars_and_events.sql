CREATE TABLE calendars (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    sport_type VARCHAR(255),
    season_year INTEGER,
    created_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_calendars_owner_id ON calendars(owner_id);

CREATE TABLE events (
    id UUID PRIMARY KEY,
    calendar_id UUID NOT NULL,
    title VARCHAR(255) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    competition_level VARCHAR(255),
    location VARCHAR(255),
    external_url VARCHAR(500),
    priority VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_events_calendar FOREIGN KEY (calendar_id) REFERENCES calendars(id) ON DELETE CASCADE
);

CREATE INDEX idx_events_calendar_start ON events(calendar_id, start_date);

CREATE TABLE event_disciplines (
    event_id UUID NOT NULL,
    position INTEGER NOT NULL,
    discipline VARCHAR(255) NOT NULL,
    PRIMARY KEY (event_id, position),
    CONSTRAINT fk_event_disciplines_event FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE
);

CREATE INDEX idx_event_disciplines_event_id ON event_disciplines(event_id);
