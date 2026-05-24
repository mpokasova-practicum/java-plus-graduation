CREATE SCHEMA IF NOT EXISTS stats_analyzer;

CREATE TABLE IF NOT EXISTS stats_analyzer.user_actions (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id BIGINT NOT NULL,
    event_id BIGINT NOT NULL,
    user_score DOUBLE PRECISION NOT NULL,
    timestamp_action TIMESTAMP NOT NULL
);

CREATE TABLE stats_analyzer.event_similarities (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    event_a BIGINT NOT NULL,
    event_b BIGINT NOT NULL,
    score DOUBLE PRECISION NOT NULL
);

