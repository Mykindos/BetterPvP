package me.mykindos.betterpvp.progression.profession.skill.woodcutting.treetactician;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.progression.profession.woodcutting.event.PlayerChopLogEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

@BPvPListener
@Singleton
public class TreeTacticianListener implements Listener {

    @Inject
    private TreeTactician skill;

    @EventHandler
    public void onPlayerChopsLog(PlayerChopLogEvent event) {
        if (skill.getSkillLevel(event.getPlayer()) <= 0) return;
        skill.onPlayerChopsLog(event);
    }
}
