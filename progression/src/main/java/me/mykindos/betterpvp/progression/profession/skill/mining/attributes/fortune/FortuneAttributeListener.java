package me.mykindos.betterpvp.progression.profession.skill.mining.attributes.fortune;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.framework.blocktag.BlockTagManager;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.progression.profession.mining.event.PlayerMinesOreEvent;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

@BPvPListener
@Singleton
public class FortuneAttributeListener implements Listener {

    private final FortuneAttribute attribute;
    private final BlockTagManager blockTagManager;

    @Inject
    public FortuneAttributeListener(FortuneAttribute attribute, BlockTagManager blockTagManager) {
        this.attribute = attribute;
        this.blockTagManager = blockTagManager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMinesOre(PlayerMinesOreEvent event) {
        Block block = event.getMinedOreBlock();
        if (!UtilBlock.isOre(block.getType())) return;
        if (blockTagManager.isPlayerPlaced(block)) return;

        double chance = attribute.getChance(event.getPlayer());
        if (chance <= 0) return;
        if (Math.random() >= chance) return;

        event.setDoubledDrops(true);
    }
}
