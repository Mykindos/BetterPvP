package me.mykindos.betterpvp.progression.profession.skill.woodcutting.barkbounty;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.progression.profession.woodcutting.event.PlayerStripLogEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

@BPvPListener
@Singleton
public class BarkBountyListener implements Listener {

    @Inject
    private BarkBounty skill;

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void whenPlayerStripsALog(PlayerStripLogEvent event) {
        if (skill.getSkillLevel(event.getPlayer()) <= 0) return;
        skill.whenPlayerStripsALog(event);
    }
}
