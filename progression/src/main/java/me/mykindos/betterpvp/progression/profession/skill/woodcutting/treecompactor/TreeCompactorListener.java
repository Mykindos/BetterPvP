package me.mykindos.betterpvp.progression.profession.skill.woodcutting.treecompactor;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.CraftItemEvent;

@BPvPListener
@Singleton
public class TreeCompactorListener implements Listener {

    @Inject
    private TreeCompactor skill;

    @EventHandler
    public void onPlaceCompactedLog(BlockPlaceEvent event) {
        if (skill.getSkillLevel(event.getPlayer()) <= 0) return;
        skill.onPlaceCompactedLog(event);
    }

    @EventHandler
    public void onCompactedLogCraft(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (skill.getSkillLevel(player) <= 0) return;
        skill.onCompactedLogCraft(event);
    }
}
