package me.mykindos.betterpvp.progression.profession.skill.fishing.swiftness;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.progression.profession.fishing.event.PlayerStartFishingEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

@BPvPListener
@Singleton
public class SwiftnessListener implements Listener {

    @Inject
    private Swiftness skill;

    @EventHandler
    public void onStartFishing(PlayerStartFishingEvent event) {
        if (skill.getSkillLevel(event.getPlayer()) <= 0) return;
        skill.onStartFishing(event);
    }
}
