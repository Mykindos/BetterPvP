package me.mykindos.betterpvp.progression.profession.skill.woodcutting.attributes.extralogs;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.framework.blocktag.BlockTagManager;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.progression.profession.woodcutting.event.PlayerChopLogEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

@BPvPListener
@Singleton
public class ExtraLogsAttributeListener implements Listener {

    private final WoodcuttingExtraLogsAttribute attribute;
    private final BlockTagManager blockTagManager;

    @Inject
    public ExtraLogsAttributeListener(WoodcuttingExtraLogsAttribute attribute, BlockTagManager blockTagManager) {
        this.attribute = attribute;
        this.blockTagManager = blockTagManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerChopLog(PlayerChopLogEvent event) {
        if (blockTagManager.isPlayerPlaced(event.getChoppedLogBlock())) return;
        if (!roll(attribute.getChance(event.getPlayer()))) return;

        event.setAdditionalLogsDropped(event.getAdditionalLogsDropped() + 1);
    }

    private boolean roll(double chance) {
        if (chance <= 0) return false;
        if (chance > 1) return Math.random() * 100.0 < chance;
        return Math.random() < chance;
    }
}
