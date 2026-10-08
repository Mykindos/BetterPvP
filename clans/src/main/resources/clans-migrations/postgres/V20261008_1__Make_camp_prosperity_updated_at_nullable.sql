-- Nothing writes updated_at any more. It stays until no server runs a build that still writes it.
ALTER TABLE camp_prosperity ALTER COLUMN updated_at DROP NOT NULL;
