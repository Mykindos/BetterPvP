package me.mykindos.betterpvp.core.utilities;

import me.mykindos.betterpvp.core.utilities.events.FetchNearbyEntityEvent;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.Collectors;

public class UtilEntity {

    public static List<LivingEntity> getNearbyEntities(Player player, double radius) {
        return getNearbyEntities(player, player.getLocation(), radius);
    }

    public static List<LivingEntity> getNearbyEntities(Player player, Location location, double radius) {
        List<LivingEntity> livingEntities = player.getWorld().getLivingEntities().stream()
                .filter(livingEntity -> livingEntity.getLocation().distance(location) <= radius && !livingEntity.equals(player))
                .collect(Collectors.toList());
        FetchNearbyEntityEvent<LivingEntity> fetchNearbyEntityEvent = new FetchNearbyEntityEvent<>(player, location, livingEntities);
        UtilServer.callEvent(fetchNearbyEntityEvent);

        return fetchNearbyEntityEvent.getEntities();
    }

}
