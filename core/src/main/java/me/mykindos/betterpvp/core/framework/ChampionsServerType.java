package me.mykindos.betterpvp.core.framework;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.kyori.adventure.text.Component;

/**
 * Server type for the Champions mini-game — the skill-based PvP game mode.
 * Unlike {@link ClansServerType} there is no squad size concept.
 */
@AllArgsConstructor
@Getter
public class ChampionsServerType implements SelectableServerType {
    private final String name;
    private final String serverNamePrefix;
    private final Component displayTitle;
}
