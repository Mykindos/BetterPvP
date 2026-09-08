-- Cutscenes the game declares, published to the console on boot.
--
-- Written BY THE GAME, like game_items and game_zones: cutscenes are built in
-- code, so code is the source of truth and this is how the console learns which
-- ids exist. That is what lets a console-authored quest or conversation point at
-- a cutscene through a validated picker rather than a free-text id, with no
-- publish step between writing a cutscene and referencing it.

CREATE TABLE IF NOT EXISTS game_cutscenes (
  key          text PRIMARY KEY,
  display_name text   NOT NULL,
  beats        text[] NOT NULL DEFAULT '{}'
);
