package me.mykindos.betterpvp.core.item.component.impl.repair;

import com.google.common.base.Preconditions;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.item.component.AbstractItemComponent;
import me.mykindos.betterpvp.core.item.component.impl.durability.DurabilityComponent;
import org.bukkit.Bukkit;

/**
 * Marks an item as repairable on the anvil and tracks how much durability has been
 * restored over the item's entire lifetime.
 * <br>
 * An item can be repaired until the cumulative restored durability reaches
 * {@code lifetimeMultiplier * maxDamage} (default 3x its initial durability). Once
 * that budget is exhausted the item can never be repaired again.
 * <br>
 * Only {@link #restoredLifetime} is persisted; the cap is derived live from the
 * {@link DurabilityComponent}'s {@code maxDamage}, mirroring how durability already
 * treats the {@code BaseItem} value as authoritative.
 */
@Getter
@Setter
public class RepairableComponent extends AbstractItemComponent {

    /** Cumulative durability restored across every repair this item has received. */
    private int restoredLifetime;

    public RepairableComponent() {
        super("repairable");
    }

    /**
     * Records durability restored by a repair.
     *
     * @param amount the durability points restored (non-negative)
     */
    public void addRestored(int amount) {
        Preconditions.checkArgument(amount >= 0, "Restored amount must be non-negative");
        this.restoredLifetime += amount;
    }

    /**
     * The total durability this item is allowed to have restored over its lifetime.
     *
     * @param maxDamage          the item's current max durability
     * @param lifetimeMultiplier how many times {@code maxDamage} may be restored in total
     */
    public int lifetimeCap(int maxDamage, int lifetimeMultiplier) {
        return maxDamage * lifetimeMultiplier;
    }

    /**
     * The durability budget still available for future repairs.
     */
    public int remainingBudget(int maxDamage, int lifetimeMultiplier) {
        return Math.max(0, lifetimeCap(maxDamage, lifetimeMultiplier) - restoredLifetime);
    }

    /**
     * Whether this item still has lifetime budget left to be repaired.
     */
    public boolean isRepairable(int maxDamage, int lifetimeMultiplier) {
        return remainingBudget(maxDamage, lifetimeMultiplier) > 0;
    }

    @Override
    public RepairableComponent copy() {
        final RepairableComponent component = new RepairableComponent();
        component.setRestoredLifetime(restoredLifetime);
        return component;
    }
}
