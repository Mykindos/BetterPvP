package me.mykindos.betterpvp.core.loot;

import lombok.Data;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.client.stats.impl.core.LootChestStat;
import me.mykindos.betterpvp.core.droptables.DropTable;
import me.mykindos.betterpvp.core.droptables.DropTableItemStack;
import me.mykindos.betterpvp.core.framework.BPvPPlugin;
import me.mykindos.betterpvp.core.framework.events.items.SpecialItemDropEvent;
import me.mykindos.betterpvp.core.utilities.UtilItem;
import me.mykindos.betterpvp.core.utilities.UtilMath;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

@Data
public class LootChest {

    private final Entity entity;
    private final String source;
    private DropTable guaranteedDrop;
    private final DropTable droptable;
    private final int numberOfDrops;
    private final long dropDelay;
    private final long dropInterval;

    /**
     *
     * @param plugin
     * @param player the player that opened this lootChest
     */
    public void onOpen(BPvPPlugin plugin, Player player, ClientManager clientManager) {

        for (int i = 0; i < numberOfDrops; i++) {
            UtilServer.runTaskLater(plugin, false, () -> {
                dropItem(player, droptable.random(), clientManager);
            }, dropDelay + (i * dropInterval));
        }

        long removalDelay = dropDelay + (numberOfDrops * dropInterval);

        if (guaranteedDrop != null) {
            DropTableItemStack item = guaranteedDrop.random();
            if(item != null) {
                UtilServer.runTaskLater(plugin, false, () -> {
                    dropItem(player, item, clientManager);
                }, removalDelay);
            }

            removalDelay += dropInterval;
        }
        clientManager.incrementStat(player, LootChestStat.builder().source(source).build(), 1);
        UtilServer.runTaskLater(plugin, entity::remove, removalDelay + 20L);
    }

    private void dropItem(Player player, DropTableItemStack itemstack, ClientManager clientManager) {
        entity.getWorld().playSound(entity.getLocation(), "betterpvp:chest.drop-item", 1, 1);
        final ItemStack itemStack = itemstack.create();
        final String itemName = UtilItem.getItemIdentifier(itemStack);
        final LootChestStat itemStat = LootChestStat.builder()
                .source(source)
                .item(itemName)
                .build();
        clientManager.incrementStat(player, itemStat, itemStack.getAmount());

        var item = entity.getWorld().dropItem(entity.getLocation(), itemStack);
        UtilServer.callEvent(new SpecialItemDropEvent(item, source));
        item.setVelocity(new Vector(UtilMath.randDouble(-0.15, 0.15), UtilMath.randDouble(0.30, 0.45), UtilMath.randDouble(-0.15, 0.15)));
    }

}
