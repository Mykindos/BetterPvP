package me.mykindos.betterpvp.progression.profession.skill.mining.gildeddiscovery;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.progression.profession.mining.event.PlayerMinesOreEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

@BPvPListener
@Singleton
public class GildedDiscoveryListener implements Listener {

    @Inject
    private GildedDiscovery skill;

    @EventHandler
    public void onBlockBreak(PlayerMinesOreEvent event) {
        if (skill.getSkillLevel(event.getPlayer()) <= 0) return;
        skill.onBlockBreak(event);
    }
}
