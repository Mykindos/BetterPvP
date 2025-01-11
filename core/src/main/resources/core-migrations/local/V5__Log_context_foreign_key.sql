DELETE FROM logs_context
WHERE LogId NOT IN (SELECT id FROM logs);

ALTER TABLE logs_context
    ADD CONSTRAINT fk_logs
        FOREIGN KEY (LogId)
            REFERENCES logs (id)
            ON DELETE CASCADE;
