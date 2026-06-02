package me.mykindos.betterpvp.core.scene.mob.faction;

/**
 * How one {@link Faction} regards another. Used by the mob-vs-mob relation matrix and bridged
 * into the entity relationship events so existing skills/targeting treat mobs consistently.
 */
public enum FactionRelation {
    ALLY,
    NEUTRAL,
    ENEMY
}
