package me.mykindos.betterpvp.progression.profession.skill.fishing.attributes.catchspeednearby;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.progression.profession.fishing.event.PlayerStartFishingEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

@BPvPListener
@Singleton
public class CatchSpeedNearbyAttributeListener implements Listener {

    private final CatchSpeedNearbyAttribute attribute;

    @Inject
    public CatchSpeedNearbyAttributeListener(CatchSpeedNearbyAttribute attribute) {
        this.attribute = attribute;
    }

    @EventHandler
    public void onStartFishing(PlayerStartFishingEvent event) {
        Player caster = event.getPlayer();

        // Find the highest nearby-player bonus among fishing players within 10 blocks (excluding self).
        double bestBonus = caster.getLocation().getNearbyPlayers(10).stream()
                .filter(nearby -> !nearby.equals(caster) && nearby.getFishHook() != null)
                .mapToDouble(attribute::getCatchSpeedNearbyBonus)
                .max()
                .orElse(0);

        if (bestBonus <= 0) return;

        int reduced = (int) (event.getHook().getWaitTime() * (1 - bestBonus));
        event.getHook().setWaitTime(Math.max(1, reduced));
    }
}
