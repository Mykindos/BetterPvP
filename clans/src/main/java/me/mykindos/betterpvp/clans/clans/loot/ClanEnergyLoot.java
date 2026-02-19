package me.mykindos.betterpvp.clans.clans.loot;

import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.clans.clans.core.EnergyItem;
import me.mykindos.betterpvp.core.loot.Loot;
import me.mykindos.betterpvp.core.loot.LootContext;
import me.mykindos.betterpvp.core.loot.ReplacementStrategy;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

import java.util.function.Predicate;

@EqualsAndHashCode(callSuper = true)
public abstract class ClanEnergyLoot<R> extends Loot<Integer, R> {

    protected final EnergyItem energyType;
    protected final int minAmount;
    protected final int maxAmount;
    protected final boolean autoDeposit;

    ClanEnergyLoot(EnergyItem energyType, ReplacementStrategy replacementStrategy, Predicate<LootContext> condition, int minAmount, int maxAmount, boolean autoDeposit) {
        super(replacementStrategy, condition);
        this.energyType = energyType;
        this.minAmount = minAmount;
        this.maxAmount = maxAmount;
        this.autoDeposit = autoDeposit;
    }

    /** Returns the max energy amount, used for display purposes. */
    @Override
    public Integer getReward() {
        return maxAmount;
    }

    @Override
    public ItemView getIcon() {
        return ItemView.builder()
                .material(energyType.getMaterial())
                .displayName(Component.text(energyType.getName(), TextColor.color(227, 156, 255)))
                .build();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{energyType=" + energyType + ", min=" + minAmount + ", max=" + maxAmount + "}";
    }

    public static GivenClanEnergyLoot given(EnergyItem energyType, int minAmount, int maxAmount) {
        return new GivenClanEnergyLoot(energyType, ReplacementStrategy.UNSET, context -> true, minAmount, maxAmount, false);
    }

    public static GivenClanEnergyLoot given(EnergyItem energyType, ReplacementStrategy replacementStrategy, int minAmount, int maxAmount, boolean autoDeposit) {
        return new GivenClanEnergyLoot(energyType, replacementStrategy, context -> true, minAmount, maxAmount, autoDeposit);
    }

    public static DroppedClanEnergyLoot dropped(EnergyItem energyType, int minAmount, int maxAmount) {
        return new DroppedClanEnergyLoot(energyType, ReplacementStrategy.UNSET, context -> true, minAmount, maxAmount, false);
    }

    public static DroppedClanEnergyLoot dropped(EnergyItem energyType, ReplacementStrategy replacementStrategy, int minAmount, int maxAmount, boolean autoDeposit) {
        return new DroppedClanEnergyLoot(energyType, replacementStrategy, context -> true, minAmount, maxAmount, autoDeposit);
    }
}
