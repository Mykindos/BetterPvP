package me.mykindos.betterpvp.core.world.construction;

import com.fasterxml.jackson.annotation.JsonAlias;

public enum JobKind {
    BUILD,
    @JsonAlias("UPGRADE")
    ADVANCE,
    MOVE,
    REPAIR
}
