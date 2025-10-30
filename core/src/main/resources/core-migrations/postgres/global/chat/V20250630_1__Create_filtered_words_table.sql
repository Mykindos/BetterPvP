CREATE TABLE IF NOT EXISTS filtered_words (
    id SERIAL PRIMARY KEY,
    word VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by int null references clients(id)
);
