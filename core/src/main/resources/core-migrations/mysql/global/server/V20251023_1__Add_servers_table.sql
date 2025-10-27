CREATE TABLE IF NOT EXISTS servers
(
    id   TINYINT UNSIGNED,
    Name VARCHAR(255) NOT NULL,
    PRIMARY KEY (Name),
    UNIQUE KEY (id)
);

INSERT IGNORE INTO servers (id, Name) VALUES (0, "Champions");
INSERT IGNORE INTO servers (id, Name) VALUES (1, "Clans-1");
INSERT IGNORE INTO servers (id, Name) VALUES (2, "Clans-2");
INSERT IGNORE INTO servers (id, Name) VALUES (3, "Clans-3");
INSERT IGNORE INTO servers (id, Name) VALUES (4, "Clans-4");
INSERT IGNORE INTO servers (id, Name) VALUES (5, "Clans-5");
INSERT IGNORE INTO servers (id, Name) VALUES (6, "Clans-6");
INSERT IGNORE INTO servers (id, Name) VALUES (7, "Clans-7");
INSERT IGNORE INTO servers (id, Name) VALUES (8, "Clans-8");
INSERT IGNORE INTO servers (id, Name) VALUES (9, "Clans-9");
INSERT IGNORE INTO servers (id, Name) VALUES (10, "Clans-10");