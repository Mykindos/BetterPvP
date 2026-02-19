package me.mykindos.betterpvp.core.loot.economy;

import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.framework.economy.CoinItem;
import me.mykindos.betterpvp.core.loot.Loot;
import me.mykindos.betterpvp.core.loot.LootContext;
import me.mykindos.betterpvp.core.loot.ReplacementStrategy;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

import java.util.function.Predicate;

@EqualsAndHashCode(callSuper = true)
public abstract class CoinLoot<R> extends Loot<Integer, R> {

    protected final CoinItem coinType;
    protected final int minAmount;
    protected final int maxAmount;

    CoinLoot(CoinItem coinType, ReplacementStrategy replacementStrategy, Predicate<LootContext> condition, int minAmount, int maxAmount) {
        super(replacementStrategy, condition);
        this.coinType = coinType;
        this.minAmount = minAmount;
        this.maxAmount = maxAmount;
    }

    /** Returns the max coin amount, used for display purposes. */
    @Override
    public Integer getReward() {
        return maxAmount;
    }

    @Override
    public ItemView getIcon() {
        return ItemView.builder()
                .material(coinType.getMaterial())
                .displayName(Component.text(coinType.getName(), TextColor.color(255, 215, 0)))
                .build();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{coinType=" + coinType + ", min=" + minAmount + ", max=" + maxAmount + "}";
    }

    public static GivenCoinLoot given(CoinItem coinType, int minAmount, int maxAmount) {
        return new GivenCoinLoot(coinType, ReplacementStrategy.UNSET, context -> true, minAmount, maxAmount);
    }

    public static GivenCoinLoot given(CoinItem coinType, ReplacementStrategy replacementStrategy, int minAmount, int maxAmount) {
        return new GivenCoinLoot(coinType, replacementStrategy, context -> true, minAmount, maxAmount);
    }

    public static DroppedCoinLoot dropped(CoinItem coinType, int minAmount, int maxAmount) {
        return new DroppedCoinLoot(coinType, ReplacementStrategy.UNSET, context -> true, minAmount, maxAmount);
    }

    public static DroppedCoinLoot dropped(CoinItem coinType, ReplacementStrategy replacementStrategy, int minAmount, int maxAmount) {
        return new DroppedCoinLoot(coinType, replacementStrategy, context -> true, minAmount, maxAmount);
    }
}
