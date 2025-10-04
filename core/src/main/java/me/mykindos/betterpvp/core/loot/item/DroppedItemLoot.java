package me.mykindos.betterpvp.core.loot.item;

import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.framework.events.items.SpecialItemLootEvent;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.loot.LootContext;
import me.mykindos.betterpvp.core.loot.ReplacementStrategy;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Item;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

@EqualsAndHashCode(callSuper = true)
public final class DroppedItemLoot extends ItemLoot<Item> {

    DroppedItemLoot(NamespacedKey itemKey, ReplacementStrategy replacementStrategy, Predicate<LootContext> condition, int minAmount, int maxAmount) {
        super(itemKey, replacementStrategy, condition, minAmount, maxAmount);
    }

    /**
     * Awards this loot to the given context.
     *
     * @param context The context to award the loot to.
     */
    @Override
    public Item award(LootContext context) {
        final Location location = context.getLocation();
        final ItemInstance reward = this.getReward();
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
