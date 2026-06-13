-- Quest-giver NPC definitions (authored in the console) + a manifest of the
-- game's registered scene-object factories (written by the game on boot).
--
-- Placement comes from Mapper data-points: a PointRegion named with a quest NPC
-- id is spawned as that NPC. The NPC's appearance/kind/binding live here.

-- Console-authored NPC definitions. `source` = 'factory' (spawn via a registered
-- factory + type, like /npc spawn) or 'human' (a HumanNPC with a custom skin).
-- `kind` + `content_id` are the interaction binding (start a conversation/quest).
CREATE TABLE IF NOT EXISTS quest_npcs (
  id             text PRIMARY KEY,
  display_name   text NOT NULL DEFAULT 'NPC',
  source         text NOT NULL DEFAULT 'human',   -- factory | human
  factory        text,            -- when source=factory
  type           text,            -- when source=factory
  skin_value     text,            -- when source=human (texture value)
  skin_signature text,            -- when source=human (texture signature)
  updated_at     timestamptz NOT NULL DEFAULT now()
);

-- Game manifest: which factories + types exist (for the console's picker).
-- Rebuilt by the game on boot.
CREATE TABLE IF NOT EXISTS game_npc_factories (
  factory text NOT NULL,
  type    text NOT NULL,
  PRIMARY KEY (factory, type)
);
