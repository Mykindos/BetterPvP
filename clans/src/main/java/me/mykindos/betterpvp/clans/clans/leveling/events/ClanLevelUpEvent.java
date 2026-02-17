package me.mykindos.betterpvp.clans.clans.leveling.events;

import lombok.Data;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;

/**
 * Fired after a clan's level increases as a result of XP gain.
 * Not cancellable — the level-up is already committed before this event fires.
 * Any system that needs to react to level-up (ceremonies, logs, achievement checks) should listen to this event.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ClanLevelUpEvent extends CustomEvent {

    private final Clan clan;
    private final long previousLevel;
    private final long newLevel;

}
