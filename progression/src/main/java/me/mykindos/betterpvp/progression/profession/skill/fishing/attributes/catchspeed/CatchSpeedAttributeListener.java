package me.mykindos.betterpvp.progression.profession.skill.fishing.attributes.catchspeed;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.progression.profession.fishing.event.PlayerStartFishingEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

@BPvPListener
@Singleton
public class CatchSpeedAttributeListener implements Listener {

    private final CatchSpeedAttribute attribute;

    @Inject
    public CatchSpeedAttributeListener(CatchSpeedAttribute attribute) {
        this.attribute = attribute;
    }

    @EventHandler
    public void onStartFishing(PlayerStartFishingEvent event) {
        double bonus = attribute.getCatchSpeedBonus(event.getPlayer());
        if (bonus <= 0) return;

        int reduced = (int) (event.getHook().getWaitTime() * (1 - bonus));
        event.getHook().setWaitTime(Math.max(1, reduced));
    }
}
