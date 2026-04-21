package me.mykindos.betterpvp.progression.profession.skill.woodcutting.attributes.compactedlog;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.framework.blocktag.BlockTagManager;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemRegistry;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilItem;
import me.mykindos.betterpvp.progression.profession.woodcutting.event.PlayerChopLogEvent;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.Objects;

@BPvPListener
@Singleton
public class CompactedLogAttributeListener implements Listener {

    private final WoodcuttingCompactedLogAttribute attribute;
    private final BlockTagManager blockTagManager;
    private final ItemFactory itemFactory;
    private final ItemRegistry itemRegistry;

    @Inject
    public CompactedLogAttributeListener(WoodcuttingCompactedLogAttribute attribute, BlockTagManager blockTagManager,
                                         ItemFactory itemFactory, ItemRegistry itemRegistry) {
        this.attribute = attribute;
        this.blockTagManager = blockTagManager;
        this.itemFactory = itemFactory;
        this.itemRegistry = itemRegistry;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerChopLog(PlayerChopLogEvent event) {
        if (blockTagManager.isPlayerPlaced(event.getChoppedLogBlock())) return;
        if (!roll(attribute.getChance(event.getPlayer()))) return;

        BaseItem compactedLog = Objects.requireNonNull(itemRegistry.getItem(new NamespacedKey("progression", "compacted_log")));
        UtilItem.insert(event.getPlayer(), itemFactory.create(compactedLog).createItemStack());
    }

    private boolean roll(double chance) {
        if (chance <= 0) return false;
        if (chance > 1) return Math.random() * 100.0 < chance;
        return Math.random() < chance;
    }
}
