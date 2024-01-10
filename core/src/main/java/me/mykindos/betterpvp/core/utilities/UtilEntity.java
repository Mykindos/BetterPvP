package me.mykindos.betterpvp.core.utilities;

import com.google.common.base.Preconditions;
import me.mykindos.betterpvp.core.framework.customtypes.CustomArmourStand;
import me.mykindos.betterpvp.core.framework.customtypes.KeyValue;
import me.mykindos.betterpvp.core.utilities.events.EntityProperty;
import me.mykindos.betterpvp.core.utilities.events.FetchNearbyEntityEvent;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.craftbukkit.v1_20_R3.CraftWorld;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public class UtilEntity {

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
        List<KeyValue<LivingEntity, EntityProperty>> livingEntities = new ArrayList<>();
        source.getWorld().getLivingEntities().stream()
                .filter(livingEntity -> {
                    if(!source.getWorld().getName().equalsIgnoreCase(livingEntity.getWorld().getName())) return false;
                    if (livingEntity.equals(source)) return false;
                    if (livingEntity.getLocation().distance(location) > radius) return false;
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
}
