package me.mykindos.betterpvp.core.utilities;

import me.mykindos.betterpvp.core.utilities.events.EntityProperty;
import me.mykindos.betterpvp.core.utilities.events.FetchNearbyEntityEvent;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.Collectors;

public class UtilEntity {

    public static List<LivingEntity> getNearbyEntities(Player player, double radius) {
        return getNearbyEntities(player, player.getLocation(), radius, EntityProperty.ALL);
    }

    public static List<LivingEntity> getNearbyEntities(LivingEntity source, Location location, double radius, EntityProperty entityProperty) {
        List<LivingEntity> livingEntities = source.getWorld().getLivingEntities().stream()
                .filter(livingEntity -> {
                    if (livingEntity.equals(source)) return false;
                    if (livingEntity.getLocation().distance(location) > radius) return false;
                    return !(livingEntity instanceof ArmorStand);
                })
                .collect(Collectors.toList());

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

}
