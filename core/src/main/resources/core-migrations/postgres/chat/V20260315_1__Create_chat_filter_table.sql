CREATE TABLE IF NOT EXISTS chat_filter (
    id SERIAL PRIMARY KEY,
    word VARCHAR(255) NOT NULL UNIQUE,
    created_at BIGINT NOT NULL,
    created_by BIGINT NULL REFERENCES clients(id)
);