package me.mykindos.betterpvp.core.loot;

import lombok.Data;
import me.mykindos.betterpvp.core.framework.BPvPPlugin;
import me.mykindos.betterpvp.core.framework.events.items.SpecialItemDropEvent;
import me.mykindos.betterpvp.core.utilities.UtilMath;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.model.WeighedList;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

@Data
public class LootChest {

    private final Entity entity;
    private final String source;
    private final WeighedList<ItemStack> droptable;
    private final int numberOfDrops;
    private final long dropDelay;
    private final long dropInterval;

    public void onOpen(BPvPPlugin plugin) {
        for (int i = 0; i < numberOfDrops; i++) {
            UtilServer.runTaskLater(plugin, false, () -> {
                entity.getWorld().playSound(entity.getLocation(), "betterpvp:chest.drop-item", 1, 1);
                var item = entity.getWorld().dropItem(entity.getLocation(), droptable.random());
                UtilServer.callEvent(new SpecialItemDropEvent(item, source));
                item.setVelocity(new Vector(UtilMath.randDouble(-0.15, 0.15), UtilMath.randDouble(0.35, 0.5), UtilMath.randDouble(-0.15, 0.15)));

            }, dropDelay + (i * dropInterval));
        }

        UtilServer.runTaskLater(plugin, entity::remove, dropDelay + (numberOfDrops * dropInterval) + 20L);
    }

}
