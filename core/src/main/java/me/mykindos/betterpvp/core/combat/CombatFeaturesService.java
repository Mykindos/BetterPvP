package me.mykindos.betterpvp.core.combat;

import com.google.common.collect.MapMaker;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.combat.events.PlayerCombatFeatureStateChangeEvent;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.Set;

@Singleton
public class CombatFeaturesService {

    private final Set<Player> inactive = Collections.newSetFromMap(new MapMaker().weakKeys().makeMap());

    public boolean isActive(Player player) {
        return !inactive.contains(player);
    }

    public void setActive(Player player, boolean active) {
        final boolean previous = isActive(player);
        if (previous == active) {
            return;
        }

        if (active) {
            this.inactive.remove(player);
        } else {
            this.inactive.add(player);
        }

        UtilServer.callEvent(new PlayerCombatFeatureStateChangeEvent(player, previous, active));
    }

    public void clear(Player player) {
        inactive.remove(player);
    }
}
