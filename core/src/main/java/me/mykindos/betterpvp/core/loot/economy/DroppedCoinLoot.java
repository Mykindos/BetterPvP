package me.mykindos.betterpvp.core.loot.economy;

import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.framework.economy.CoinItem;
import me.mykindos.betterpvp.core.loot.LootContext;
import me.mykindos.betterpvp.core.loot.ReplacementStrategy;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

@EqualsAndHashCode(callSuper = true)
public final class DroppedCoinLoot extends CoinLoot<Item> {

    DroppedCoinLoot(CoinItem coinType, ReplacementStrategy replacementStrategy, Predicate<LootContext> condition, int minAmount, int maxAmount) {
        super(coinType, replacementStrategy, condition, minAmount, maxAmount);
    }

    @Override
    public Item award(LootContext context) {
        int amount = (minAmount == maxAmount) ? minAmount : ThreadLocalRandom.current().nextInt(minAmount, maxAmount + 1);
        ItemStack item = coinType.generateItem(amount);
        return context.getLocation().getWorld().dropItemNaturally(context.getLocation(), item);
    }
}
