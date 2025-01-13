UPDATE
    champions_builds
SET Sword    = SUBSTRING_INDEX(SUBSTRING_INDEX(Sword, ',', 1), ',', -1),
    Axe      = SUBSTRING_INDEX(SUBSTRING_INDEX(Axe, ',', 1), ',', -1),
    Bow      = SUBSTRING_INDEX(SUBSTRING_INDEX(Bow, ',', 1), ',', -1),
    PassiveA = SUBSTRING_INDEX(SUBSTRING_INDEX(PassiveA, ',', 1), ',', -1),
    PassiveB = SUBSTRING_INDEX(SUBSTRING_INDEX(PassiveB, ',', 1), ',', -1),
    Global   = SUBSTRING_INDEX(SUBSTRING_INDEX(Global, ',', 1), ',', -1);