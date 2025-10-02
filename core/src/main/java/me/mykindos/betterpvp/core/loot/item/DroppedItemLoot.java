package me.mykindos.betterpvp.core.loot.item;

import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.framework.events.items.SpecialItemLootEvent;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.loot.LootContext;
import me.mykindos.betterpvp.core.loot.ReplacementStrategy;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.Location;
import org.bukkit.entity.Item;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

public final class DroppedItemLoot extends ItemLoot<Item> {

    DroppedItemLoot(BaseItem reward, ReplacementStrategy replacementStrategy, Predicate<LootContext> condition, int minAmount, int maxAmount) {
        super(reward, replacementStrategy, condition, minAmount, maxAmount);
    }

    /**
     * Awards this loot to the given context.
     *
     * @param context The context to award the loot to.
     */
    @Override
    public Item award(LootContext context) {
        final Location location = context.getLocation();
        final Core plugin = JavaPlugin.getPlugin(Core.class);
        final ItemFactory itemFactory = plugin.getInjector().getInstance(ItemFactory.class);
        final ItemInstance reward = itemFactory.create(this.getReward());
        int count;
        if (minAmount == maxAmount) {
            count = minAmount;
        } else {
            count = ThreadLocalRandom.current().nextInt(minAmount, maxAmount + 1);
        }
        reward.getItemStack().setAmount(count);
        UtilServer.callEvent(new SpecialItemLootEvent(context, reward, context.getSource()));
        return location.getWorld().dropItemNaturally(location, reward.createItemStack());
    }
}
