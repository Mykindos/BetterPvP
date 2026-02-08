package me.mykindos.betterpvp.core.utilities.model.selector.entity;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.events.EntityProperty;
import me.mykindos.betterpvp.core.utilities.model.selector.origin.EntityOrigin;
import org.bukkit.GameMode;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;

/**
 * Provides common entity filters for use with entity selectors.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class EntityFilters {

    /**
     * A filter that excludes armor stands.
     *
     * @return a filter that rejects armor stands
     */
    public static EntityFilter noArmorStands() {
        return (origin, entity) -> !(entity instanceof ArmorStand);
    }

    /**
     * A filter that only accepts players.
     *
     * @return a filter that only accepts players
     */
    public static EntityFilter playersOnly() {
        return (origin, entity) -> entity instanceof Player;
    }

    /**
     * A filter that excludes players in creative or spectator mode.
     *
     * @return a filter that rejects invulnerable players
     */
    public static EntityFilter noInvulnerablePlayers() {
        return (origin, entity) -> {
            if (entity instanceof Player player) {
                GameMode mode = player.getGameMode();
                return mode != GameMode.CREATIVE && mode != GameMode.SPECTATOR;
            }
            return true;
        };
    }

    /**
     * A filter that excludes the source entity if the origin is entity-based.
     *
     * @return a filter that rejects the source entity
     */
    public static EntityFilter excludeSource() {
        return (origin, entity) -> {
            if (origin instanceof EntityOrigin entityOrigin) {
                return !entity.equals(entityOrigin.entity());
            }
            return true;
        };
    }

    /**
     * A filter that only accepts entities that are enemies of the origin entity.
     * Uses the game's relationship system to determine enemy status.
     * If the origin is not entity-based, all entities are considered enemies.
     *
     * @return a filter that only accepts enemies
     */
    public static EntityFilter enemiesOnly() {
        return (origin, entity) -> {
            if (origin instanceof EntityOrigin entityOrigin) {
                return UtilEntity.IS_ENEMY.test(entityOrigin.entity(), entity);
            }
            // If no source entity, consider all as potential enemies
            return true;
        };
    }

    /**
     * A filter that only accepts entities that are friendly to the origin entity.
     * Uses the game's relationship system to determine friendly status.
     * If the origin is not entity-based, no entities are considered friendly.
     *
     * @return a filter that only accepts friendlies
     */
    public static EntityFilter friendliesOnly() {
        return (origin, entity) -> {
            if (origin instanceof EntityOrigin entityOrigin) {
                return UtilEntity.getRelation(entityOrigin.entity(), entity) == EntityProperty.FRIENDLY;
            }
            // If no source entity, consider none as friendly
            return false;
        };
    }

    /**
     * A filter based on the game's entity property system.
     *
     * @param property the entity property to filter by
     * @return a filter that accepts entities matching the property
     */
    public static EntityFilter byProperty(EntityProperty property) {
        return switch (property) {
            case ALL -> EntityFilter.all();
            case ENEMY -> enemiesOnly();
            case FRIENDLY -> friendliesOnly();
        };
    }

    /**
     * The standard combat filter that excludes:
     * - The source entity (self)
     * - Armor stands
     * - Players in creative/spectator mode
     *
     * @return the standard combat filter
     */
    public static EntityFilter combat() {
        return excludeSource()
                .and(noArmorStands())
                .and(noInvulnerablePlayers());
    }

    /**
     * The standard combat filter that also only includes enemies.
     * Combines the combat filter with the enemies-only filter.
     *
     * @return the combat enemies filter
     */
    public static EntityFilter combatEnemies() {
        return combat().and(enemiesOnly());
    }

    /**
     * The standard combat filter that also only includes friendlies.
     * Combines the combat filter with the friendlies-only filter.
     *
     * @return the combat friendlies filter
     */
    public static EntityFilter combatFriendlies() {
        return combat().and(friendliesOnly());
    }
}
