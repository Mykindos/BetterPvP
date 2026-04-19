package me.mykindos.betterpvp.progression.profession.skill.mining.smelter;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.progression.profession.mining.event.PlayerMinesOreEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

@BPvPListener
@Singleton
public class SmelterListener implements Listener {

    @Inject
    private Smelter skill;

    @EventHandler
    public void onBlockBreak(PlayerMinesOreEvent event) {
        if (skill.getSkillLevel(event.getPlayer()) <= 0) return;
        skill.onBlockBreak(event);
    }
}
