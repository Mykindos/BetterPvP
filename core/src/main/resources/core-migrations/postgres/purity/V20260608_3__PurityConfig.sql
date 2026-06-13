-- Purity / rune-slot tuning tables with a draft -> published split: the console
-- edits `draft`, Publish copies it to `published`, and the GAME only reads
-- `published`. Seeded with sensible defaults (draft = published). ON CONFLICT
-- DO NOTHING keeps re-runs safe and preserves edits.

CREATE TABLE IF NOT EXISTS purity_distributions (
  name         text PRIMARY KEY,
  draft        jsonb NOT NULL,
  published    jsonb,
  updated_at   timestamptz NOT NULL DEFAULT now(),
  published_at timestamptz
);

CREATE TABLE IF NOT EXISTS purity_reforge_bias (
  purity       text PRIMARY KEY,
  draft        jsonb NOT NULL,
  published    jsonb,
  updated_at   timestamptz NOT NULL DEFAULT now(),
  published_at timestamptz
);

CREATE TABLE IF NOT EXISTS purity_rune_slot_distributions (
  purity       text PRIMARY KEY,
  draft        jsonb NOT NULL,
  published    jsonb,
  updated_at   timestamptz NOT NULL DEFAULT now(),
  published_at timestamptz
);

-- ── Purity distribution (rarity weights) ────────────────────────────────────
INSERT INTO purity_distributions (name, draft) VALUES
('default', '{"distribution_name":"default","weights":{"PITIFUL":5,"FRAGILE":20,"MODERATE":35,"POLISHED":25,"PRISTINE":12,"PERFECT":3}}')
ON CONFLICT (name) DO NOTHING;

-- ── Reforge bias (beta-distribution alpha/beta per purity) ───────────────────
INSERT INTO purity_reforge_bias (purity, draft) VALUES
('PITIFUL',  '{"purity":"PITIFUL","alpha":0.3,"beta":3.0,"notes":"Heavily favors minimum stats"}'),
('FRAGILE',  '{"purity":"FRAGILE","alpha":0.6,"beta":2.4,"notes":"Favors low stats"}'),
('MODERATE', '{"purity":"MODERATE","alpha":1.2,"beta":1.6,"notes":"Slight minimum bias"}'),
('POLISHED', '{"purity":"POLISHED","alpha":1.6,"beta":1.2,"notes":"Slight maximum bias"}'),
('PRISTINE', '{"purity":"PRISTINE","alpha":2.4,"beta":0.6,"notes":"Favors high stats"}'),
('PERFECT',  '{"purity":"PERFECT","alpha":3.0,"beta":0.3,"notes":"Heavily favors maximum stats"}')
ON CONFLICT (purity) DO NOTHING;

-- ── Rune-slot distribution (socket count weights per purity) ─────────────────
INSERT INTO purity_rune_slot_distributions (purity, draft) VALUES
('PITIFUL',  '{"purity":"PITIFUL","socket_weights":{"0":60,"1":30,"2":8,"3":2,"4":0},"max_socket_weights":{"0":50,"1":35,"2":12,"3":3,"4":0},"notes":"Rarely more than one socket"}'),
('FRAGILE',  '{"purity":"FRAGILE","socket_weights":{"0":40,"1":35,"2":18,"3":6,"4":1},"max_socket_weights":{"0":30,"1":38,"2":22,"3":8,"4":2},"notes":""}'),
('MODERATE', '{"purity":"MODERATE","socket_weights":{"0":20,"1":30,"2":30,"3":15,"4":5},"max_socket_weights":{"0":15,"1":28,"2":32,"3":18,"4":7},"notes":""}'),
('POLISHED', '{"purity":"POLISHED","socket_weights":{"0":8,"1":20,"2":32,"3":28,"4":12},"max_socket_weights":{"0":5,"1":15,"2":30,"3":32,"4":18},"notes":""}'),
('PRISTINE', '{"purity":"PRISTINE","socket_weights":{"0":3,"1":10,"2":25,"3":37,"4":25},"max_socket_weights":{"0":2,"1":7,"2":20,"3":36,"4":35},"notes":""}'),
('PERFECT',  '{"purity":"PERFECT","socket_weights":{"0":1,"1":2,"2":10,"3":35,"4":52},"max_socket_weights":{"0":1,"1":2,"2":7,"3":25,"4":65},"notes":"Heavily favors 3-4 sockets"}')
ON CONFLICT (purity) DO NOTHING;

-- Publish the seeded defaults so the game has live values out of the box.
UPDATE purity_distributions            SET published = draft, published_at = now() WHERE published IS NULL;
UPDATE purity_reforge_bias             SET published = draft, published_at = now() WHERE published IS NULL;
UPDATE purity_rune_slot_distributions  SET published = draft, published_at = now() WHERE published IS NULL;
