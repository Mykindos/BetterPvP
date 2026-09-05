package me.mykindos.betterpvp.clans.world.mine;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemRegistry;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Counts and spends the bloomstone in a player's inventory.
 * <p>
 * Bloomstone is not held in a balance anywhere — it is an item in a bag, which is the point of it: it can be lost,
 * carried, and seen. So a trade has to read the inventory, and {@link #take} has to be all-or-nothing, since a partial
 * deduction on a full-price purchase is the one failure here nobody could put right.
 */
@Singleton
public class MineCurrency {

    /** The item the training mine pays out in. */
    public static final String BLOOMSTONE = "core:bloomstone";

    private final ItemRegistry itemRegistry;
    private final ItemFactory itemFactory;

    @Inject
    public MineCurrency(@NotNull ItemRegistry itemRegistry, @NotNull ItemFactory itemFactory) {
        this.itemRegistry = itemRegistry;
        this.itemFactory = itemFactory;
    }

    /** How much bloomstone {@code player} is carrying. */
    public int count(@NotNull Player player) {
        final BaseItem bloomstone = itemRegistry.getItem(BLOOMSTONE);
        if (bloomstone == null) {
            return 0;
        }
        int total = 0;
        for (ItemStack stack : player.getInventory().getStorageContents()) {
            if (stack != null && itemFactory.isItemOfType(stack, bloomstone)) {
                total += stack.getAmount();
            }
        }
        return total;
    }

    /**
     * Removes {@code amount} bloomstone, or nothing at all.
     *
     * @return false if the player did not have enough, in which case their inventory is untouched
     */
    public boolean take(@NotNull Player player, int amount) {
        final BaseItem bloomstone = itemRegistry.getItem(BLOOMSTONE);
        if (bloomstone == null || count(player) < amount) {
            return false;
        }
        int remaining = amount;
        final ItemStack[] contents = player.getInventory().getStorageContents();
        for (int slot = 0; slot < contents.length && remaining > 0; slot++) {
            final ItemStack stack = contents[slot];
            if (stack == null || !itemFactory.isItemOfType(stack, bloomstone)) {
                continue;
            }
            final int taken = Math.min(remaining, stack.getAmount());
            remaining -= taken;
            if (taken >= stack.getAmount()) {
                player.getInventory().setItem(slot, null);
            } else {
                stack.setAmount(stack.getAmount() - taken);
            }
        }
        return true;
    }
}
