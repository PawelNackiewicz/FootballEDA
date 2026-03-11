CREATE TABLE event_store (
    id BIGSERIAL PRIMARY KEY,
    event_id UUID NOT NULL UNIQUE,
    match_id UUID NOT NULL,
    event_type VARCHAR(30) NOT NULL,
    payload TEXT NOT NULL,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX idx_event_store_match ON event_store(match_id, occurred_at);
CREATE INDEX idx_event_store_type ON event_store(event_type);
CREATE INDEX idx_event_store_event_id ON event_store(event_id);
