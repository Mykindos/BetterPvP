CREATE INDEX idx_kills_killer ON kills (Killer);
CREATE INDEX idx_kills_victim ON kills (Victim);
CREATE INDEX idx_kills_killer_victim ON kills (Killer, Victim);
CREATE INDEX idx_kill_contributions_contributor ON kill_contributions (Contributor);