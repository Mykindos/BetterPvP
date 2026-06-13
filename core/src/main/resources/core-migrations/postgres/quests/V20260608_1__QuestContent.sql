-- Authored-content backbone + game manifest + quest runtime tables.
--
-- The content + manifest tables mirror the admin console's db/sql contract
-- (the console writes authored content into `content`; the game reads
-- `content.published`). The manifest tables are written BY THE GAME on boot
-- (code is the source of truth). The quest_* runtime tables are game-owned.
--
-- IF NOT EXISTS keeps this safe when a dev console has already applied db/sql
-- to the same database.

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ── Authored content (DB → game) ──────────────────────────────────────────
CREATE TABLE IF NOT EXISTS content (
  id           uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  type         text NOT NULL,
  name         text NOT NULL,
  description  text,
  status       text NOT NULL DEFAULT 'draft',
  draft        jsonb NOT NULL DEFAULT '{}'::jsonb,
  published    jsonb,
  version      integer NOT NULL DEFAULT 0,
  revision     integer NOT NULL DEFAULT 0,
  created_at   timestamptz NOT NULL DEFAULT now(),
  created_by   uuid,
  updated_at   timestamptz NOT NULL DEFAULT now(),
  updated_by   uuid,
  published_at timestamptz,
  published_by uuid
);
CREATE INDEX IF NOT EXISTS content_type_status_idx ON content (type, status);

CREATE TABLE IF NOT EXISTS content_snapshots (
  id         uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  content_id uuid NOT NULL REFERENCES content(id) ON DELETE CASCADE,
  version    integer NOT NULL,
  payload    jsonb NOT NULL,
  label      text,
  created_at timestamptz NOT NULL DEFAULT now(),
  created_by uuid
);

CREATE TABLE IF NOT EXISTS content_links (
  from_content_id uuid NOT NULL REFERENCES content(id) ON DELETE CASCADE,
  to_content_id   uuid NOT NULL,
  kind            text NOT NULL,
  PRIMARY KEY (from_content_id, to_content_id, kind)
);

-- ── Game manifest (game → DB; code is the source of truth) ──────────────────
CREATE TABLE IF NOT EXISTS game_items (
  key text PRIMARY KEY,
  display_name text NOT NULL,
  source text NOT NULL DEFAULT 'vanilla',
  material text,
  tags text[] NOT NULL DEFAULT '{}',
  updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE IF NOT EXISTS game_zones (
  key text PRIMARY KEY,
  display_name text NOT NULL,
  world text,
  tags text[] NOT NULL DEFAULT '{}'
);
CREATE TABLE IF NOT EXISTS game_professions (
  key text PRIMARY KEY,
  display_name text NOT NULL,
  max_level integer
);
CREATE TABLE IF NOT EXISTS quest_primitives (
  id text PRIMARY KEY,
  category text NOT NULL,
  label text NOT NULL,
  param_schema jsonb NOT NULL DEFAULT '{}'::jsonb,
  ui jsonb NOT NULL DEFAULT '{}'::jsonb
);

-- ── Quest runtime (game-owned) ──────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS quest_instances (
  id             uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  quest_id       uuid NOT NULL,
  scope_type     text NOT NULL,
  scope_id       text NOT NULL,
  status         text NOT NULL DEFAULT 'active',
  current_stages jsonb NOT NULL DEFAULT '[]'::jsonb,
  started_at     timestamptz NOT NULL DEFAULT now(),
  updated_at     timestamptz NOT NULL DEFAULT now(),
  UNIQUE (quest_id, scope_type, scope_id)
);
CREATE INDEX IF NOT EXISTS quest_instances_scope_idx ON quest_instances (scope_type, scope_id);

CREATE TABLE IF NOT EXISTS quest_objective_progress (
  instance_id   uuid NOT NULL REFERENCES quest_instances(id) ON DELETE CASCADE,
  objective_key text NOT NULL,
  progress      integer NOT NULL DEFAULT 0,
  target        integer NOT NULL DEFAULT 1,
  PRIMARY KEY (instance_id, objective_key)
);

CREATE TABLE IF NOT EXISTS quest_flags (
  scope_type text NOT NULL,
  scope_id   text NOT NULL,
  flag       text NOT NULL,
  value      text,
  PRIMARY KEY (scope_type, scope_id, flag)
);
