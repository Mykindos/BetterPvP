package me.mykindos.betterpvp.core.loot.entity;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import me.mykindos.betterpvp.core.loot.Loot;
import me.mykindos.betterpvp.core.loot.LootContext;
import me.mykindos.betterpvp.core.loot.ReplacementStrategy;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Zombie;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Comparator;
import java.util.function.Predicate;

/**
 * Loot that spawns an entity at the location of the given {@link LootContext}.
 *
 * <p>JSON shape:
 * <pre>
 * { "type": "entity_spawn", "entityType": "DROWNED", "launchAtSource": true, "pdcMarkerKey": "progression:swimmer" }
 * </pre>
 */
@Getter
@EqualsAndHashCode(callSuper = true)
public final class EntitySpawnLoot extends Loot<EntityType, Entity> {

    private final EntityType entityType;
    /** Whether to fling the spawned entity toward the nearest player within 10 blocks. */
    private final boolean launchAtSource;
    /** If non-null, a BOOLEAN PDC entry with this namespaced key is set on the spawned entity. */
    @Nullable
    private final String pdcMarkerKey;

    public EntitySpawnLoot(EntityType entityType, boolean launchAtSource, @Nullable String pdcMarkerKey,
                           ReplacementStrategy replacementStrategy, Predicate<LootContext> condition) {
        super(replacementStrategy, condition);
        this.entityType = entityType;
        this.launchAtSource = launchAtSource;
        this.pdcMarkerKey = pdcMarkerKey;
    }

    /**
     * Factory for a swimmer — an entity that launches toward the nearest player and is tagged
     * with the {@code progression:swimmer} PDC key so downstream listeners can identify it.
     */
    public static EntitySpawnLoot swimmer(EntityType entityType) {
        return new EntitySpawnLoot(entityType, true, "progression:swimmer",
                ReplacementStrategy.UNSET, ctx -> true);
    }

    @Override
    public EntityType getReward() {
        return entityType;
    }

    @Override
    protected Entity award(LootContext context) {
        final Location location = context.getLocation();

        // Find the nearest player within 10 blocks to use as the launch target.
        Player target = null;
        if (launchAtSource) {
            Collection<Player> nearby = location.getNearbyEntitiesByType(Player.class, 10.0);
            target = nearby.stream()
                    .min(Comparator.comparingDouble(p -> p.getLocation().distanceSquared(location)))
                    .orElse(null);
        }

        final Player finalTarget = target;
        final Entity[] spawned = new Entity[1];

        location.getWorld().spawnEntity(location, entityType, CreatureSpawnEvent.SpawnReason.CUSTOM, entity -> {
            if (entity instanceof Zombie zombie) {
                zombie.setShouldBurnInDay(false);
            }
            if (entity instanceof Skeleton skeleton) {
                skeleton.setShouldBurnInDay(false);
            }
            if (entity instanceof LivingEntity livingEntity) {
                livingEntity.setCanPickupItems(false);
            }

            if (launchAtSource && finalTarget != null) {
                final Vector direction = finalTarget.getLocation().subtract(location)
                        .toVector()
                        .normalize()
                        .multiply(1.5)
                        .add(new Vector(0, 0.25, 0));
                entity.setVelocity(direction);
            }

            if (pdcMarkerKey != null) {
                final NamespacedKey key = NamespacedKey.fromString(pdcMarkerKey);
                if (key != null) {
                    entity.getPersistentDataContainer().set(key, PersistentDataType.BOOLEAN, true);
                }
            }

            spawned[0] = entity;
        });

        return spawned[0];
    }

    @Override
    public ItemView getIcon() {
        final Material material = resolveSpawnEgg(entityType);
        return ItemView.builder()
                .material(material)
                .displayName(Component.translatable(entityType.translationKey()))
                .build();
    }

    private static Material resolveSpawnEgg(EntityType type) {
        final String eggName = type.name() + "_SPAWN_EGG";
        try {
            return Material.valueOf(eggName);
        } catch (IllegalArgumentException ignored) {
            return Material.ZOMBIE_SPAWN_EGG;
        }
    }

    @Override
    public String toString() {
        return "EntitySpawnLoot{entityType=" + entityType + ", launchAtSource=" + launchAtSource + '}';
    }
}
