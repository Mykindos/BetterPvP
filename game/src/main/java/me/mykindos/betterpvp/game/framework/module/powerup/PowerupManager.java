package me.mykindos.betterpvp.game.framework.module.powerup;

import com.google.inject.Inject;
import dev.brauw.mapper.region.PointRegion;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.game.GamePlugin;
import me.mykindos.betterpvp.game.framework.ServerController;
import me.mykindos.betterpvp.game.framework.manager.MapManager;
import me.mykindos.betterpvp.game.framework.model.Lifecycled;
import me.mykindos.betterpvp.game.framework.model.player.PlayerController;
import me.mykindos.betterpvp.game.framework.model.setting.hotbar.HotBarLayoutManager;
import me.mykindos.betterpvp.game.framework.model.stats.StatManager;
import me.mykindos.betterpvp.game.framework.module.powerup.impl.Restock;
import me.mykindos.betterpvp.game.guice.GameScoped;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.HandlerList;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

/**
 * Manages powerup points that allow players to refresh their inventory
 */
@CustomLog
@GameScoped
public class PowerupManager implements Lifecycled {

    private final Set<Powerup> powerups = new HashSet<>();
    private final MapManager mapManager;
    private final HotBarLayoutManager layoutManager;
    private final GamePlugin plugin;
    private final PowerupListener listener;
    private final Map<String, Function<PointRegion, Powerup>> powerupTypes = new HashMap<>();

    @Inject
    public PowerupManager(MapManager mapManager, ServerController serverController,
                          HotBarLayoutManager layoutManager, PlayerController playerController,
                          GamePlugin plugin, ClientManager clientManager, StatManager statManager) {
        this.listener =  new PowerupListener(this, serverController, playerController, plugin);
        this.mapManager = mapManager;
        this.layoutManager = layoutManager;
        this.plugin = plugin;

        powerupTypes.put("restock", region -> new Restock(region.getLocation(), plugin, layoutManager, clientManager, statManager));
    }

    public void registerPowerupType(String type, Function<PointRegion, Powerup> factory) {
        powerupTypes.put(type, factory);
    }

    @Override
    public void setup() {
        // Load all powerup points
        for (Map.Entry<String, Function<PointRegion, Powerup>> entry : powerupTypes.entrySet()) {
            loadPowerupType(entry.getKey(), entry.getValue());
        }

        activateAllPowerups();
        Bukkit.getPluginManager().registerEvents(listener, plugin);
    }

    private void loadPowerupType(String type, Function<PointRegion, Powerup> factory) {
        // Load powerup type
        List<PointRegion> regions = mapManager.getCurrentMap().findRegion(type, PointRegion.class).toList();
        for (PointRegion region : regions) {
            Powerup point = factory.apply(region);
            powerups.add(point);
            point.setup();
            log.info("Created {} point at {}", type, region.getLocation()).submit();
        }
    }

    @Override
    public void tearDown() {
        HandlerList.unregisterAll(listener);

        // Cleanup all powerup points
        for (Powerup point : powerups) {
            point.tearDown();
        }
        powerups.clear();
    }

    /**
     * Activates all restock points in the map
     */
    private void activateAllPowerups() {
        for (Powerup point : powerups) {
            point.activate();
        }
    }

    /**
     * Gets a powerup point by a location
     *
     * @param location location
     * @param radius The radius to search for the powerup point
     * @return The powerup point or null if not found
     */
    public Powerup getPowerupByNearestLocation(Location location, double radius) {
        return powerups.stream()
                .filter(powerup -> Objects.equals(powerup.getLocation().getWorld(), location.getWorld()))
                .filter(powerup -> powerup.getLocation().distanceSquared(location) <= radius * radius)
                .findFirst()
                .orElse(null);
    }

    public Set<Powerup> getPowerups() {
        return Collections.unmodifiableSet(powerups);
    }
}