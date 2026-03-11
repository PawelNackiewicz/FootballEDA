CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE teams (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    short_name VARCHAR(10) NOT NULL,
    logo_url VARCHAR(255)
);

CREATE TABLE players (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    number INT NOT NULL,
    position VARCHAR(20) NOT NULL,
    team_id UUID NOT NULL REFERENCES teams(id)
);

CREATE TABLE matches (
    id UUID PRIMARY KEY,
    home_team_id UUID NOT NULL REFERENCES teams(id),
    away_team_id UUID NOT NULL REFERENCES teams(id),
    home_score INT DEFAULT 0,
    away_score INT DEFAULT 0,
    status VARCHAR(20) DEFAULT 'NOT_STARTED',
    current_minute INT DEFAULT 0,
    started_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE match_stats (
    id UUID PRIMARY KEY,
    match_id UUID NOT NULL REFERENCES matches(id),
    team_id UUID NOT NULL REFERENCES teams(id),
    possession DOUBLE PRECISION DEFAULT 50.0,
    shots INT DEFAULT 0,
    shots_on_target INT DEFAULT 0,
    fouls INT DEFAULT 0,
    corners INT DEFAULT 0,
    yellow_cards INT DEFAULT 0,
    red_cards INT DEFAULT 0,
    expected_goals DOUBLE PRECISION DEFAULT 0.0,
    UNIQUE(match_id, team_id)
);

CREATE TABLE standings (
    id UUID PRIMARY KEY,
    team_id UUID NOT NULL UNIQUE REFERENCES teams(id),
    team_name VARCHAR(100) NOT NULL,
    played INT DEFAULT 0,
    won INT DEFAULT 0,
    drawn INT DEFAULT 0,
    lost INT DEFAULT 0,
    goals_for INT DEFAULT 0,
    goals_against INT DEFAULT 0,
    points INT DEFAULT 0
);

CREATE TABLE processed_events (
    id BIGSERIAL PRIMARY KEY,
    event_id UUID NOT NULL,
    consumer_name VARCHAR(50) NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(event_id, consumer_name)
);

CREATE INDEX idx_processed_events_lookup ON processed_events(event_id, consumer_name);
