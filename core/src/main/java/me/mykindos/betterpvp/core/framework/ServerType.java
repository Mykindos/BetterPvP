package me.mykindos.betterpvp.core.framework;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Represents a server type within the network
 */
@AllArgsConstructor
@Getter
public enum ServerType {

    CLANS_CLASSIC,
    CLANS_SQUADS,
    CLANS_CASUAL,
    HUB,

}
