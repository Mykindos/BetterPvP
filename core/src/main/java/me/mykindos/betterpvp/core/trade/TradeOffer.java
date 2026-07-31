package me.mykindos.betterpvp.core.trade;

import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.inventory.inventory.VirtualInventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * One side of a trade: what a player has put on the table, and whether they have accepted.
 * <p>
 * Offered items live in this inventory rather than in the player's, so they are already out of their
 * hands before anything settles. That is what makes the swap a hand-over of goods already held rather
 * than a two-sided withdrawal that could half-succeed. Currencies cannot be escrowed the same way -
 * they are only a promised amount here, and are re-checked at settlement.
 */
@Getter
public class TradeOffer {

    public static final int SLOTS = 16;

    private final UUID player;
    private final VirtualInventory items = new VirtualInventory(SLOTS);
    private final Map<TradeCurrency, Integer> currencies = new LinkedHashMap<>();

    @Setter
    private boolean accepted;

    public TradeOffer(@NotNull UUID player) {
        this.player = player;
    }

    public int getCurrency(@NotNull TradeCurrency currency) {
        return currencies.getOrDefault(currency, 0);
    }

    /**
     * Sets the promised amount of a currency. An amount of zero or less takes it off the table.
     */
    public void setCurrency(@NotNull TradeCurrency currency, int amount) {
        if (amount <= 0) {
            currencies.remove(currency);
        } else {
            currencies.put(currency, amount);
        }
    }

    @NotNull
    public Map<TradeCurrency, Integer> getCurrencies() {
        return Collections.unmodifiableMap(currencies);
    }

    public boolean isEmpty() {
        if (!currencies.isEmpty()) {
            return false;
        }
        for (ItemStack item : items.getItems()) {
            if (item != null && !item.getType().isAir()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Takes every escrowed item out of this offer, leaving it empty.
     * <p>
     * Draining is the only way items leave an offer, so an item can be handed to exactly one
     * destination - the other player on settlement, or its owner on cancel - and never to both.
     */
    @NotNull
    public List<ItemStack> drainItems() {
        final List<ItemStack> drained = new ArrayList<>(SLOTS);
        final ItemStack[] contents = items.getItems();
        for (int slot = 0; slot < contents.length; slot++) {
            final ItemStack item = contents[slot];
            if (item == null || item.getType().isAir()) {
                continue;
            }
            drained.add(item);
            items.setItem(null, slot, null);
        }
        return drained;
    }

    /**
     * @return How many inventory slots the holder of this offer needs to receive it.
     */
    public int getOccupiedSlots() {
        int occupied = 0;
        for (ItemStack item : items.getItems()) {
            if (item != null && !item.getType().isAir()) {
                occupied++;
            }
        }
        return occupied;
    }
}
