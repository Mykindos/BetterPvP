package me.mykindos.betterpvp.progression.profession.skill.fishing.expertbaiter;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.progression.profession.fishing.event.PlayerThrowBaitEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

@BPvPListener
@Singleton
public class ExpertBaiterListener implements Listener {

    @Inject
    private ExpertBaiter skill;

    @EventHandler
    public void onThrowBait(PlayerThrowBaitEvent event) {
        if (skill.getSkillLevel(event.getPlayer()) <= 0) return;
        skill.onThrowBait(event);
    }
}
