package me.mykindos.betterpvp.core.scene.mob;

/**
 * The default stance a {@link SceneMob} takes toward other entities.
 * <ul>
 *   <li>{@link #FRIENDLY} - aids its owner/allies; may be owned by a player or clan.</li>
 *   <li>{@link #NEUTRAL} - minds its own business (wanders); only fights back when attacked.</li>
 *   <li>{@link #HOSTILE} - actively seeks out and attacks enemies on sight.</li>
 * </ul>
 * Disposition is a coarse default; fine-grained ally/enemy decisions are resolved through the
 * faction system and the relationship events (see
 * {@link me.mykindos.betterpvp.core.utilities.events.GetEntityRelationshipEvent}).
 */
public enum Disposition {
    FRIENDLY,
    NEUTRAL,
    HOSTILE
}
