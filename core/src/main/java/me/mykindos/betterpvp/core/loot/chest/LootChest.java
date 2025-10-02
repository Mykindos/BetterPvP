package me.mykindos.betterpvp.core.loot.chest;

import lombok.Data;
import me.mykindos.betterpvp.core.framework.BPvPPlugin;
import me.mykindos.betterpvp.core.loot.Loot;
import me.mykindos.betterpvp.core.loot.LootBundle;
import me.mykindos.betterpvp.core.loot.LootContext;
import me.mykindos.betterpvp.core.loot.LootTable;
import me.mykindos.betterpvp.core.loot.session.LootSession;
import me.mykindos.betterpvp.core.utilities.UtilMath;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;

@Data
public class LootChest {

    private final BPvPPlugin plugin;
    private final LootSession session;
    private final Entity entity;
    private final String source;
    private final LootTable lootTable;
    private final long dropDelay;
    private final long dropInterval;

    public void open(Player player) {
        final LootContext context = new LootContext(player, entity.getLocation(), session, source);
        final LootBundle lootBundle = this.lootTable.generateLoot(context);
        final int dropCount = lootBundle.getSize();

        final Iterator<@NotNull Loot<?, ?>> iterator = lootBundle.getLoot().iterator();
        for (int i = 0; i < dropCount; i++) {
            final Loot<?, ?> next = iterator.next();
            UtilServer.runTaskLater(plugin, false, () -> {
                dropItem(context, next);
            }, dropDelay + (i * dropInterval));
        }

        long removalDelay = dropDelay + (dropCount * dropInterval);
        UtilServer.runTaskLater(plugin, entity::remove, removalDelay + 20L);
    }

    private void dropItem(LootContext context, Loot<?, ?> loot) {
        entity.getWorld().playSound(entity.getLocation(), "betterpvp:chest.drop-item", 1, 1);
        if (loot.award(context) instanceof Item item) {
            item.setVelocity(new Vector(UtilMath.randDouble(-0.15, 0.15), UtilMath.randDouble(0.30, 0.45), UtilMath.randDouble(-0.15, 0.15)));
        }
    }

}
