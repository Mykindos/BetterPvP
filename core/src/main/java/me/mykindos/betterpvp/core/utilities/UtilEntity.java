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
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.craftbukkit.v1_20_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_20_R3.entity.CraftEntity;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
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

    public static boolean isRemoved(@NotNull Entity entity) {
        return ((CraftEntity) entity).getHandle().isRemoved();
    }

    public static EntityRemovalReason getRemovalReason(@NotNull Entity entity) {
        final net.minecraft.world.entity.Entity handle = ((CraftEntity) entity).getHandle();
        Preconditions.checkArgument(handle.isRemoved(), "Entity must be removed");
        return switch(Objects.requireNonNull(handle.getRemovalReason())) {
            case KILLED -> EntityRemovalReason.KILLED;
            case DISCARDED -> EntityRemovalReason.DISCARDED;
            case UNLOADED_TO_CHUNK -> EntityRemovalReason.UNLOADED_TO_CHUNK;
            case UNLOADED_WITH_PLAYER -> EntityRemovalReason.UNLOADED_TO_PLAYER;
            case CHANGED_DIMENSION -> EntityRemovalReason.DIMENSION_CHANGED;
        };
    }

    public static final BiPredicate<Player, Entity> IS_ENEMY = (player, entity) -> {
        if (!(entity instanceof LivingEntity) || entity.equals(player) || UtilPlayer.isCreativeOrSpectator(entity)) {
            return false;
        }

        if (!(entity instanceof Player other)) {
            return true;
        }

        return getRelation(player, other) != EntityProperty.FRIENDLY;
    };

    public static boolean isEntityFriendly(LivingEntity entity, LivingEntity target) {
        return getRelation(entity, target) == EntityProperty.FRIENDLY;
    }

    public static EntityProperty getRelation(LivingEntity entity, LivingEntity target) {
        return UtilServer.callEvent(new GetEntityRelationshipEvent(entity, target)).getEntityProperty();
    }

    public static Optional<Entity> getEntity(@NotNull World world, int id) {
        return Optional.ofNullable(((CraftWorld) world).getHandle().getEntity(id))
                .map(net.minecraft.world.entity.Entity::getBukkitEntity);
    }

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

    public static Optional<RayTraceResult> interpolateCollision(@NotNull Location lastLocation, @NotNull Location destination, float raySize, @Nullable Predicate<Entity> entityFilter) {
        Preconditions.checkNotNull(lastLocation, "Last location cannot be null");
        Preconditions.checkNotNull(destination, "Destination cannot be null");
        Preconditions.checkArgument(destination.getWorld() == lastLocation.getWorld(), "Locations must be in the same world");

        final Vector directionRaw = destination.toVector().subtract(lastLocation.toVector());
        final double distance = directionRaw.length();
        final Vector direction = distance < 1e-6 ? lastLocation.getDirection() : directionRaw.normalize();
        return Optional.ofNullable(lastLocation.getWorld().rayTraceEntities(lastLocation,
                direction,
                distance,
                raySize,
                entityFilter));
    }

    public static List<KeyValue<LivingEntity, EntityProperty>> getNearbyEntities(LivingEntity source, double radius) {
        return getNearbyEntities(source, source.getLocation(), radius, EntityProperty.ALL);
    }

    public static List<LivingEntity> getNearbyEnemies(LivingEntity source, Location location, double radius){
        List<LivingEntity> enemies = new ArrayList<>();
        getNearbyEntities(source, location, radius, EntityProperty.ENEMY).forEach(entry -> enemies.add(entry.get()));
        return enemies;
    }

    public static List<KeyValue<LivingEntity, EntityProperty>> getNearbyEntities(LivingEntity source, Location location, double radius, EntityProperty entityProperty) {
        if(!source.getWorld().equals(location.getWorld())) return new ArrayList<>();
        List<KeyValue<LivingEntity, EntityProperty>> livingEntities = new ArrayList<>();
        source.getWorld().getLivingEntities().stream()
                .filter(livingEntity -> {
                    if (livingEntity.equals(source)) return false;
                    if (livingEntity.getLocation().distance(location) > radius) return false;
                    if(livingEntity instanceof Player player) {
                        if(player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
                            return false;
                        }
                    }
                    return !(livingEntity instanceof ArmorStand);
                })
                .forEach(ent -> livingEntities.add(new KeyValue<>(ent, entityProperty)));

        FetchNearbyEntityEvent<LivingEntity> fetchNearbyEntityEvent = new FetchNearbyEntityEvent<>(source, location, livingEntities, entityProperty);
        UtilServer.callEvent(fetchNearbyEntityEvent);

        return fetchNearbyEntityEvent.getEntities();
    }

    public static void setHealth(LivingEntity entity, double health) {
        AttributeInstance maxHealthAttribute = entity.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (maxHealthAttribute != null) {
            entity.setHealth(Math.min(maxHealthAttribute.getValue(), health));
        }
    }

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
     * Changes the view range of the display to the specified amount of blocks
     * @param display The display to change the view range of
     * @param blocks The amount of blocks to change the view range to
     */
    public static void setViewRangeBlocks(@NotNull Display display, float blocks) {
        display.setViewRange((float) (blocks / (net.minecraft.world.entity.Entity.getViewScale() * 64.0)));
    }

    /**
     * Gets the view range of the display in blocks
     */
    public static float getViewRangeBlocks(@NotNull Display display) {
        return display.getViewRange() * (float) (net.minecraft.world.entity.Entity.getViewScale() * 64.0);
    }
}
