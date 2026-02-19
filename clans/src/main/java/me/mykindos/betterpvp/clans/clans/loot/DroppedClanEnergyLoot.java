package me.mykindos.betterpvp.clans.clans.loot;

import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.clans.clans.core.EnergyItem;
import me.mykindos.betterpvp.core.loot.LootContext;
import me.mykindos.betterpvp.core.loot.ReplacementStrategy;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

@EqualsAndHashCode(callSuper = true)
public final class DroppedClanEnergyLoot extends ClanEnergyLoot<Item> {

    DroppedClanEnergyLoot(EnergyItem energyType, ReplacementStrategy replacementStrategy, Predicate<LootContext> condition, int minAmount, int maxAmount, boolean autoDeposit) {
        super(energyType, replacementStrategy, condition, minAmount, maxAmount, autoDeposit);
    }

    @Override
    public Item award(LootContext context) {
        int amount = (minAmount == maxAmount) ? minAmount : ThreadLocalRandom.current().nextInt(minAmount, maxAmount + 1);
        ItemStack item = energyType.generateItem(amount, autoDeposit);
        return context.getLocation().getWorld().dropItemNaturally(context.getLocation(), item);
    }
}
