package me.mykindos.betterpvp.core.utilities;

import com.google.common.base.Preconditions;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import me.mykindos.betterpvp.core.framework.customtypes.CustomArmourStand;
import me.mykindos.betterpvp.core.framework.customtypes.KeyValue;
import me.mykindos.betterpvp.core.utilities.events.EntityProperty;
import me.mykindos.betterpvp.core.utilities.events.FetchNearbyEntityEvent;
import me.mykindos.betterpvp.core.utilities.events.GetEntityRelationshipEvent;
import me.mykindos.betterpvp.core.utilities.model.EntityRemovalReason;
import me.mykindos.betterpvp.core.utilities.model.MultiRayTraceResult;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityCombustByEntityEvent;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class UtilEntity {

    /**
     * Retrieves the reason for why an entity has been removed.
     *
     * @param entity the entity whose removal reason is to be retrieved; must not be null and must already be removed
     * @return the reason for the entity's removal as an {@link EntityRemovalReason}
     * @throws IllegalArgumentException if the entity is not removed
     * @throws NullPointerException if the entity or its removal reason is null
     */
    public static EntityRemovalReason getRemovalReason(@NotNull Entity entity) {
        final net.minecraft.world.entity.Entity handle = ((CraftEntity) entity).getHandle();
        Preconditions.checkArgument(isRemoved(entity), "Entity must be removed");
        return switch(Objects.requireNonNull(handle.getRemovalReason())) {
            case KILLED -> EntityRemovalReason.KILLED;
            case DISCARDED -> EntityRemovalReason.DISCARDED;
            case UNLOADED_TO_CHUNK -> EntityRemovalReason.UNLOADED_TO_CHUNK;
            case UNLOADED_WITH_PLAYER -> EntityRemovalReason.UNLOADED_TO_PLAYER;
            case CHANGED_DIMENSION -> EntityRemovalReason.DIMENSION_CHANGED;
        };
    }

    /**
     * A {@link BiPredicate} that determines if a given entity is considered an enemy to a specified player.
     *
     * The predicate returns:
     * - {@code false} if the entity is not a {@code LivingEntity}.
     * - {@code false} if the entity is the same as the player.
     * - {@code false} if the entity is in creative or spectator mode.
     * - {@code true} if the entity is not a player.
     * - {@code true} if the entity is a player but does not have a friendly relationship with the given player.
     *
     * This is typically utilized in scenarios where the game logic distinguishes between friendly and enemy entities
     * based on their type or their relationship with the player.
     */
    public static final BiPredicate<Player, Entity> IS_ENEMY = (player, entity) -> {
        if (!(entity instanceof LivingEntity) || entity.equals(player) || UtilPlayer.isCreativeOrSpectator(entity)) {
            return false;
        }

        if (!(entity instanceof Player other)) {
            return true;
        }

        return getRelation(player, other) != EntityProperty.FRIENDLY;
    };

    /**
     * Determines if the given target entity is friendly towards the specified entity.
     *
     * @param entity the primary living entity whose relationship with the target is being evaluated
     * @param target the target living entity to check against the primary entity
     * @return true if the target entity is classified as friendly towards the primary entity, false otherwise
     */
    public static boolean isEntityFriendly(LivingEntity entity, LivingEntity target) {
        return getRelation(entity, target) == EntityProperty.FRIENDLY;
    }

    /**
     * Determines the relationship property between two living entities.
     * This method triggers a {@link GetEntityRelationshipEvent} to compute
     * the relationship status between the specified entity and target.
     *
     * @param entity The source {@link LivingEntity} to evaluate the relationship from.
     * @param target The target {@link LivingEntity} to evaluate the relationship against.
     * @return The {@link EntityProperty} that represents the relationship between the source and the target.
     */
    public static EntityProperty getRelation(LivingEntity entity, LivingEntity target) {
        return UtilServer.callEvent(new GetEntityRelationshipEvent(entity, target)).getEntityProperty();
    }

    /**
     * Retrieves an entity from the given world by its unique ID.
     * Converts the entity from the internal Minecraft server representation
     * to the Bukkit Entity representation.
     *
     * @param world the world from which to retrieve the entity, must not be null
     * @param id the unique integer ID of the entity to retrieve
     * @return an Optional containing the Bukkit representation of the entity if found, otherwise an empty Optional
     */
    public static Optional<Entity> getEntity(@NotNull World world, int id) {
        return Optional.ofNullable(((CraftWorld) world).getHandle().getEntity(id))
                .map(net.minecraft.world.entity.Entity::getBukkitEntity);
    }

    /**
     * Interpolates and detects multiple collision points between two locations by tracing a ray.
     * This method determines all collision points along a ray path between the starting and
     * destination locations, with an optional filter for targeting specific entities.
     *
     * @param lastLocation the starting location of the ray (cannot be null)
     * @param destination the destination location of the ray (cannot be null)
     * @param raySize the size of the ray used for collision detection
     * @param entityFilter an optional predicate used to filter entities for collision detection (can be null)
     * @return an Optional containing a MultiRayTraceResult if collisions are detected; otherwise, an empty Optional
     */
    public static Optional<MultiRayTraceResult> interpolateMultiCollision(@NotNull Location lastLocation, @NotNull Location destination, float raySize, @Nullable Predicate<Entity> entityFilter) {
        Set<RayTraceResult> hits = new HashSet<>();
        boolean empty;
        do {
            Optional<RayTraceResult> hit = UtilEntity.interpolateCollision(lastLocation,
                    destination,
                    raySize,
                    ent -> hits.stream().noneMatch(result -> result.getHitEntity() == ent) && (entityFilter == null || entityFilter.test(ent)));
            hit.ifPresent(hits::add);
            empty = hit.isEmpty();
        } while (!empty);

        return hits.isEmpty()
                ? Optional.empty()
                : Optional.of(new MultiRayTraceResult(hits.toArray(new RayTraceResult[0])));
    }

    /**
     * Interpolates a collision check between two locations with a specified ray size, factoring in an optional
     * entity filter. This method determines if the ray intersects with any entities along the path.
     *
     * @param lastLocation the starting location of the ray. Must not be null.
     * @param destination the ending location of the ray. Must not be null and must be in the same world as lastLocation.
     * @param raySize the radius of the ray used for collision checks.
     * @param entityFilter an optional filter used to determine which entities should be considered for collision.
     *                     Can be null for no filtering.
     * @return an {@code Optional<RayTraceResult>} containing the first entity that intersects the ray, or an empty
     *         {@code Optional} if no intersection occurs.
     */
    public static Optional<RayTraceResult> interpolateCollision(@NotNull Location lastLocation, @NotNull Location destination, float raySize, @Nullable Predicate<Entity> entityFilter) {
        Preconditions.checkNotNull(lastLocation, "Last location cannot be null");
        Preconditions.checkNotNull(destination, "Destination cannot be null");
        Preconditions.checkArgument(destination.getWorld() == lastLocation.getWorld(), "Locations must be in the same world");

        final Vector directionRaw = destination.toVector().subtract(lastLocation.toVector());
        final double distance = directionRaw.length();
        final Vector direction = distance < 1e-6 ? lastLocation.getDirection() : directionRaw.normalize();
        if (!direction.toVector3d().isFinite()) {
            return destination.getNearbyEntities(raySize, raySize, raySize).stream()
                    .filter(entityFilter == null ? entity -> true : entityFilter)
                    .findFirst()
                    .map(entity -> new RayTraceResult(destination.toVector(), entity));
        }

        return Optional.ofNullable(lastLocation.getWorld().rayTraceEntities(lastLocation,
                direction,
                distance,
                raySize,
                entityFilter));
    }

    /**
     * Retrieves a list of nearby living entities within a specified radius of a given source entity.
     * Each entity in the list is mapped to an {@link EntityProperty}, which indicates its relationship
     * or type with respect to the given source.
     *
     * This method filters out entities that are the source itself, invulnerable players, or armor stands.
     *
     * @param source the source entity around which nearby entities are searched
     * @param radius the radius within which nearby entities are retrieved
     * @return a list of key-value pairs where the key is a nearby {@link LivingEntity} and the value is of type {@link EntityProperty}
     */
    public static List<KeyValue<LivingEntity, EntityProperty>> getNearbyEntities(LivingEntity source, double radius) {
        return getNearbyEntities(source, source.getLocation(), radius, EntityProperty.ALL);
    }

    /**
     * Retrieves a list of nearby enemies within a specified radius around a given location.
     *
     * @param source The living entity that serves as the source or reference point for the search.
     * @param location The location around which to search for nearby enemies.
     * @param radius The radius within which to search for enemies.
     * @return A list of nearby LivingEntity instances classified as enemies within the specified radius.
     */
    public static List<LivingEntity> getNearbyEnemies(LivingEntity source, Location location, double radius){
        List<LivingEntity> enemies = new ArrayList<>();
        getNearbyEntities(source, location, radius, EntityProperty.ENEMY).forEach(entry -> enemies.add(entry.get()));
        return enemies;
    }

    /**
     * Retrieves a list of nearby living entities around a given location within a specified radius,
     * filtering entities based on certain conditions and associating them with a provided entity property.
     * Filters out entities that are the same as the source entity, invulnerable players, and armor stands.
     * Additional filtering can be applied based on the specified {@code entityProperty}.
     *
     * @param source the source entity that is initiating the search
     * @param location the location around which to search for nearby living entities
     * @param radius the radius within which to search for nearby entities
     * @param entityProperty the property used to filter the entities (e.g., FRIENDLY, ENEMY, ALL)
     * @return a list of {@code KeyValue} objects containing nearby entities as the key and their associated properties as the value
     */
    public static List<KeyValue<LivingEntity, EntityProperty>> getNearbyEntities(LivingEntity source, Location location, double radius, EntityProperty entityProperty) {
        if(!source.getWorld().equals(location.getWorld())) return new ArrayList<>();
        List<KeyValue<LivingEntity, EntityProperty>> livingEntities = new ArrayList<>();
        UtilLocation.getNearbyLivingEntities(location, radius).stream()
                .filter(livingEntity -> {
                    if (livingEntity.equals(source)) return false;
                    if(livingEntity instanceof Player player) {
                        if(player.getGameMode().isInvulnerable()) {
                            return false;
                        }
                    }
                    return !(livingEntity instanceof ArmorStand);
                })
                .forEach(ent -> livingEntities.add(new KeyValue<>(ent, entityProperty)));

        FetchNearbyEntityEvent<LivingEntity> fetchNearbyEntityEvent = new FetchNearbyEntityEvent<>(source, location, livingEntities, entityProperty);
        UtilServer.callEvent(fetchNearbyEntityEvent);
        fetchNearbyEntityEvent.getEntities().removeIf(pair -> {
            if (fetchNearbyEntityEvent.getEntityProperty().equals(EntityProperty.ALL)) {
                return false;
            }
            return !fetchNearbyEntityEvent.getEntityProperty().equals(pair.getValue());
        });

        return fetchNearbyEntityEvent.getEntities();
    }

    /**
     * Sets the health of a given LivingEntity. The health value is capped
     * at the entity's maximum health value if it exceeds the maximum.
     *
     * @param entity the LivingEntity whose health is to be set
     * @param health the desired health value to assign to the entity
     */
    public static void setHealth(LivingEntity entity, double health) {
        AttributeInstance maxHealthAttribute = entity.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealthAttribute != null) {
            entity.setHealth(Math.min(maxHealthAttribute.getValue(), health));
        }
    }

    /**
     * Creates and spawns a utility ArmorStand at the specified location with specific attributes
     * that make it suitable for use in custom mechanics or utilities. The ArmorStand is configured
     * as small, invulnerable, non-collidable, persistent, and silent, while also disabling specific
     * unwanted interactions such as portal usage, fire visuals, and gravity effects.
     *
     * @param location the location where the ArmorStand should be spawned; cannot be null
     * @return the configured ArmorStand instance
     */
    public static ArmorStand createUtilityArmorStand(@NotNull Location location) {
        CustomArmourStand as = new CustomArmourStand(((CraftWorld) location.getWorld()).getHandle());
        ArmorStand armorStand = (ArmorStand) as.spawn(location);
        armorStand.setSmall(true);
        armorStand.setGravity(true);
        armorStand.setSilent(true); // Remove sounds the armor stand makes like when it falls or in water
        armorStand.setPortalCooldown(Integer.MAX_VALUE); // We don't want them using portals
        armorStand.setInvulnerable(true); // We don't want them taking damage
        armorStand.setVisualFire(false); // We don't want them to have fire
        armorStand.setPersistent(false); // We don't want them to be saved in the world
        armorStand.setCollidable(false); // We don't want them to collide with anything
        return armorStand;
    }

    /**
     * Sets the view range for a given display in terms of blocks.
     *
     * @param display The display for which the view range is being set. Cannot be null.
     * @param blocks The desired view range in blocks.
     */
    public static void setViewRangeBlocks(@NotNull Display display, float blocks) {
        display.setViewRange((float) (blocks / (net.minecraft.world.entity.Entity.getViewScale() * 64.0)));
    }

    /**
     * Calculates the viewing range of a display in blocks.
     *
     * @param display the Display object to retrieve the view range from; must not be null
     * @return the viewing range in blocks as a float
     */
    public static float getViewRangeBlocks(@NotNull Display display) {
        return display.getViewRange() * (float) (net.minecraft.world.entity.Entity.getViewScale() * 64.0);
    }

    /**
     * Applies a fire effect to the specified entity for a given duration, triggered by another entity.
     * This method fires an {@link EntityCombustByEntityEvent} to allow event handling and cancellation.
     * If the event is cancelled, the fire effect is not applied.
     *
     * @param damagee The entity that will be set on fire. Must not be null.
     * @param damager The entity causing the fire effect. Must not be null.
     * @param duration The duration of the fire effect in milliseconds.
     */
    public static void setFire(@NotNull Entity damagee, @NotNull Entity damager, long duration) {
        EntityCombustByEntityEvent entityCombustByEntityEvent = UtilServer.callEvent(new EntityCombustByEntityEvent(damager, damagee, (float) duration /1000L));
        if (entityCombustByEntityEvent.isCancelled()) return;
        damagee.setFireTicks((int) (entityCombustByEntityEvent.getDuration() * 20));
    }

    /**
     * Checks if the given entity has been removed.
     *
     * @param ent the entity to check, must not be null
     * @return true if the entity is removed or marked as pluginRemoved, otherwise false
     */
    public static boolean isRemoved(@NotNull Entity ent) {
        return ((CraftEntity) ent).getHandle().isRemoved();
    }

    public static void health(LivingEntity ent, double mod) {
        if (ent.isDead()) {
            return;
        }
        double health = ent.getHealth() + mod;
        if (health < 0.0D) {
            health = 0.0D;
        }
        if (health > UtilPlayer.getMaxHealth(ent)) {
            health = UtilPlayer.getMaxHealth(ent);
        }
        ent.setHealth(health);
    }
}
