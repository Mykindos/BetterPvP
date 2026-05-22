package me.mykindos.betterpvp.core.repair;

import lombok.Value;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.component.impl.durability.DurabilityComponent;
import me.mykindos.betterpvp.core.item.component.impl.repair.RepairableComponent;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * An immutable, fully-resolved description of a repair the anvil can perform with its
 * current contents. Produced by {@link RepairService#resolve} and executed by the
 * RepairOperation.
 * <br>
 * Roles are identified by item identity/type, never by anvil slot — placement order
 * and slots are irrelevant to anvil operations.
 */
@Value
public class RepairPlan {

    /**
     * Three states an anvil can surface for a damaged repairable item.
     * Only {@link #READY} actually progresses on swings; the others exist so the anvil
     * can show informational holograms without committing to a repair.
     */
    public enum Status {
        /** All conditions met — swings will consume catalyst and restore durability. */
        READY,
        /** The item has spent its lifetime repair budget; no further repairs are possible. */
        EXHAUSTED,
        /** Damaged item is present but the anvil lacks enough matching-tier reinforcements. */
        INSUFFICIENT_CATALYST
    }

    /** The damaged item being repaired (mutated in place when the repair completes). */
    ItemInstance item;

    /** Live durability component of {@link #item}. */
    DurabilityComponent durability;

    /** Live repairable component of {@link #item}. */
    RepairableComponent repairable;

    /** The Reinforcement tier required (matches the item's rarity); null when not repairable. */
    @Nullable ItemRarity catalystTier;

    /** Durability points this repair will restore (already clamped to the lifetime budget). */
    int restoreAmount;

    /** Convenience flag equal to {@code status == Status.READY} — the only state that mutates items. */
    boolean canRepair;

    /** Fine-grained outcome state; drives the hologram and whether {@link #complete} mutates items. */
    Status status;

    /** Hammer swings required to complete this repair. */
    int requiredSwings;

    /**
     * Total matching-tier reinforcement units this repair will consume. Equals the sum
     * of {@link ReinforcementConsumption#getUnitsConsumed()} across {@link #consumptions}
     * for a valid plan; preserved on non-repairable plans so the anvil can still show
     * the intended cost to the player.
     */
    int requiredReinforcements;

    /**
     * Pre-computed per-stack reinforcement consumption. Items on the anvil that are
     * <i>not</i> in this list (and are not the target) are returned untouched — only
     * the listed reinforcement units are consumed. Always non-null; empty for a
     * non-repairable plan.
     */
    List<ReinforcementConsumption> consumptions;

    /**
     * One reinforcement stack and the exact number of units to remove from it.
     * The remainder of the stack is left on the anvil.
     */
    @Value
    public static class ReinforcementConsumption {
        ItemInstance instance;
        int unitsConsumed;
    }
}
