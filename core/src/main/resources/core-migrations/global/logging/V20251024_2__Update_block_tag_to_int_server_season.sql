UPDATE chunk_block_tagging SET Server = 1, Season = 1;

ALTER TABLE chunk_block_tagging MODIFY COLUMN Server tinyint unsigned NOT NULL;
ALTER TABLE chunk_block_tagging MODIFY COLUMN Season tinyint unsigned NOT NULL;