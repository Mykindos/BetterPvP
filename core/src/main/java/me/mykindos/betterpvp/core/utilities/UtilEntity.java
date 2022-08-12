package me.mykindos.betterpvp.core.utilities;

import me.mykindos.betterpvp.core.framework.customtypes.KeyValue;
import me.mykindos.betterpvp.core.utilities.events.EntityProperty;
import me.mykindos.betterpvp.core.utilities.events.FetchNearbyEntityEvent;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;

public class UtilEntity {

    public static List<KeyValue<LivingEntity, EntityProperty>> getNearbyEntities(LivingEntity source, double radius) {
        return getNearbyEntities(source, source.getLocation(), radius, EntityProperty.ALL);
    }

    public static List<KeyValue<LivingEntity, EntityProperty>> getNearbyEnemies(LivingEntity source, Location location, double radius){
        return getNearbyEntities(source, location, radius, EntityProperty.ENEMY);
    }

    public static List<KeyValue<LivingEntity, EntityProperty>> getNearbyEntities(LivingEntity source, Location location, double radius, EntityProperty entityProperty) {
        List<KeyValue<LivingEntity, EntityProperty>> livingEntities = new ArrayList<>();
        source.getWorld().getLivingEntities().stream()
                .filter(livingEntity -> {
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

}
