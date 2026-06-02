package me.mykindos.betterpvp.core.scene.mob.faction;

import lombok.Value;

/**
 * A team identity a {@link me.mykindos.betterpvp.core.scene.mob.SceneMob} can belong to.
 * <p>
 * Factions let mobs have relationships with each other (mob-vs-mob PvE), independent of clans.
 * The {@link FactionService} owns the relation matrix between factions and resolves a faction
 * for any living entity (including mapping players/clans into a faction).
 */
@Value
public class Faction {

    /** Stable identifier, e.g. {@code "undead"} or {@code "village_guard"}. */
    String key;

    /** Human-readable name for display/debug. */
    String displayName;

}
