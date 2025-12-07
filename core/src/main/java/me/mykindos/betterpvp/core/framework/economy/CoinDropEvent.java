package me.mykindos.betterpvp.core.framework.economy;

import lombok.Data;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Event fired when a player drops coins on death.
 * This event allows modifying the percentage and flat amount of coins dropped.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CoinDropEvent extends CustomEvent {

    private static final HandlerList handlers = new HandlerList();

    private final Player player;
    private final int totalBalance;
    private double percentageBonus;
    private int flatAmountBonus;

    /**
     * Creates a new CoinDropEvent
     * @param player The player dropping coins
     * @param totalBalance The player's total balance
     */
    public CoinDropEvent(Player player, int totalBalance) {
        this.player = player;
        this.totalBalance = totalBalance;
        this.percentageBonus = 0.0;
        this.flatAmountBonus = 0;
    }

    /**
     * Gets the total amount of coins that will be dropped
     * @param basePercentage The base percentage from config
     * @return The total drop amount including bonuses
     */
    public int getTotalDropAmount(double basePercentage) {
        double effectivePercentage = basePercentage + percentageBonus;
        return (int) (totalBalance * effectivePercentage) + flatAmountBonus;
    }

    @Override
    @NotNull
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
