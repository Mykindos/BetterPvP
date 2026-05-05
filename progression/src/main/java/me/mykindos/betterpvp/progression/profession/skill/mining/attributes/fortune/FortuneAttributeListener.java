package me.mykindos.betterpvp.progression.profession.skill.mining.attributes.fortune;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.framework.blocktag.BlockTagManager;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;

@BPvPListener
@Singleton
public class FortuneAttributeListener implements Listener {

    private final FortuneAttribute attribute;
    private final ItemFactory itemFactory;
    private final BlockTagManager blockTagManager;

    @Inject
    public FortuneAttributeListener(FortuneAttribute attribute, ItemFactory itemFactory, BlockTagManager blockTagManager) {
        this.attribute = attribute;
        this.itemFactory = itemFactory;
        this.blockTagManager = blockTagManager;
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPlayerMinesOre(BlockDropItemEvent event) {
        BlockState block = event.getBlockState();
        if (!UtilBlock.isOre(block.getType())) return;
        if (blockTagManager.isPlayerPlaced(event.getBlock())) return;

        double chance = attribute.getChance(event.getPlayer());
        if (chance <= 0) return;
        if (Math.random() >= chance) return;

        for (Item drop : new HashSet<>(event.getItems())) {
            final ItemStack result = itemFactory.convertItemStack(drop.getItemStack()).orElseThrow();
            Item item = event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation().toCenterLocation(), result);
            event.getItems().add(item);
        }
    }
}
