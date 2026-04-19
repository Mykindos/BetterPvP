package me.mykindos.betterpvp.progression.profession.skill.fishing.thickerlines;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.progression.profession.fishing.event.PlayerCaughtFishEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

@BPvPListener
@Singleton
public class ThickerLinesListener implements Listener {

    @Inject
    private ThickerLines skill;

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCatchFish(PlayerCaughtFishEvent event) {
        if (skill.getSkillLevel(event.getPlayer()) <= 0) return;
        skill.onCatchFish(event);
    }
}
