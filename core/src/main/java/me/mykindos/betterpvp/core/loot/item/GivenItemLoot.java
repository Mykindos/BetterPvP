package me.mykindos.betterpvp.core.loot.item;

import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.framework.events.items.SpecialItemLootEvent;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.loot.LootContext;
import me.mykindos.betterpvp.core.loot.ReplacementStrategy;
import me.mykindos.betterpvp.core.utilities.UtilItem;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

@EqualsAndHashCode(callSuper = true)
public final class GivenItemLoot extends ItemLoot<List<ItemStack>> {

    GivenItemLoot(NamespacedKey itemKey, ReplacementStrategy replacementStrategy, Predicate<LootContext> condition, int minAmount, int maxAmount) {
        super(itemKey, replacementStrategy, condition, minAmount, maxAmount);
    }

    /**
     * Awards this loot to the given context.
     *
     * @param context The context to award the loot to.
     */
    @Override
    public List<ItemStack> award(LootContext context) {
        List<ItemStack> results = new ArrayList<>();
        context.getSession().getAudience().forEachAudience(audience -> {
            if (!(audience instanceof Player player)) {
                return;
            }

            final ItemInstance reward = this.getReward();
            int count;
            if (minAmount == maxAmount) {
                count = minAmount;
            } else {
                count = ThreadLocalRandom.current().nextInt(minAmount, maxAmount + 1);
            }
            reward.getItemStack().setAmount(count);
            final ItemStack given = reward.createItemStack();
            results.add(given);
            UtilItem.insert(player, given);
            UtilServer.callEvent(new SpecialItemLootEvent(context, reward, context.getSource()));
        });
        return results;
    }
}
