package me.mykindos.betterpvp.core.light;

import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.core.framework.manager.Manager;
import me.mykindos.betterpvp.core.utilities.UtilWorld;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Light;
import org.bukkit.entity.Player;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;


@Singleton
public class LightManager extends Manager<BPvPLight> {
    private final int waterDecrease = 1;
    public void addObject(BPvPLight light) {
        addObject(light.getId(), light);
    }

    public List<BPvPLight> getLightsOfPlayer(UUID playerID) {
        return getObjects().values().stream()
                .filter(bPvPLight -> bPvPLight.getPlayerID() == playerID)
                .sorted(Comparator.comparingInt(BPvPLight::getLevel).reversed())
                .collect(Collectors.toList());
    }

    public void updateLight(UUID playerID, Location newLocation, Location oldLocation) {
        List<BPvPLight> lights = getLightsOfPlayer(playerID);
        if (lights.isEmpty()) return;
        removeLight(oldLocation);
        addLight(lights.get(0), newLocation);
    }

    public void setLight(Player player, String source, int level) {
        if (level <= 0) {
            removeLight(player.getUniqueId(), source);
            return;
        }
        BPvPLight light = new BPvPLight(player.getUniqueId(), source, level);
        addObject(light);
        updateLight(player.getUniqueId(), player.getLocation(), player.getLocation());
    }

    public void addLight(BPvPLight light, Location location) {
        Material locationMaterial = location.getBlock().getType();
        Light lightData = (Light) Material.LIGHT.createBlockData();
        switch (locationMaterial) {
            case AIR, CAVE_AIR:
                lightData.setLevel(light.getLevel());
                break;
            case WATER:
                lightData.setLevel(Math.max(0, light.getLevel() - waterDecrease));
                lightData.setWaterlogged(true);
                break;
            default:
                return;
        }
        Bukkit.getOnlinePlayers().forEach(player -> {
            player.sendBlockChange(location, lightData);
        });
    }

    private void removeLight(Location location) {
        BlockData blockData = location.getBlock().getBlockData();
        Bukkit.getOnlinePlayers().forEach(player -> {
            player.sendBlockChange(location, blockData);
        });
    }

    public void removeLight(UUID playerID, String source) {
        Player player = Bukkit.getPlayer(playerID);
        getLightsOfPlayer(playerID).stream()
                .filter(bPvPLight -> bPvPLight.getSource().equals(source))
                .forEach(bPvPLight -> {
                    removeObject(bPvPLight.getId().toString());
                    if (player != null) {
                        removeLight(player.getLocation());
                        updateLight(playerID, player.getLocation(), player.getLocation());
                    }

                });
    }

    public void removeAllLights(UUID playerID) {
        Player player = Bukkit.getPlayer(playerID);
        getLightsOfPlayer(playerID)
                .forEach(bPvPLight -> {
                    removeObject(bPvPLight.getId().toString());
                });
        if (player != null) {
            removeLight(player.getLocation());
        }
    }

}
