-- Which cutscenes each player has finished.
--
-- Read by SkipPolicy.seenBefore, so a first viewing can be mandatory while a
-- replay is skippable. Kept as its own table rather than a client property
-- because "has this player seen X" is useful well beyond skipping - onboarding
-- gates, content analytics - and wants to be queryable across players.

CREATE TABLE IF NOT EXISTS cutscene_views (
  viewer     uuid        NOT NULL,
  cutscene   text        NOT NULL,
  first_seen timestamptz NOT NULL DEFAULT now(),
  last_seen  timestamptz NOT NULL DEFAULT now(),
  times      integer     NOT NULL DEFAULT 1,
  PRIMARY KEY (viewer, cutscene)
);

CREATE INDEX IF NOT EXISTS cutscene_views_cutscene_idx ON cutscene_views (cutscene);
