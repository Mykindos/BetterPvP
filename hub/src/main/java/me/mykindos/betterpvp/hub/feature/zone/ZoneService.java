package me.mykindos.betterpvp.hub.feature.zone;

import com.google.inject.Singleton;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.WeakHashMap;

@Singleton
public class ZoneService {

    private final Map<Player, Zone> players = new WeakHashMap<>();

    protected void enterZone(@NotNull Player player, @NotNull Zone zone) {
        Zone previous = players.get(player);
        players.put(player, zone);

        // If null means they just joined
        if (previous != null) {
            new PlayerExitZoneEvent(player, previous).callEvent();
        }
        if (previous != zone) {
            new PlayerEnterZoneEvent(player, zone).callEvent();
        }
    }

    public Zone getZone(@NotNull Player player) {
        return players.getOrDefault(player, Zone.NONE);
    }

}
