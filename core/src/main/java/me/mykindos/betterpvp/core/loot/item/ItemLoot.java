package me.mykindos.betterpvp.core.loot.item;

import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.loot.Loot;
import me.mykindos.betterpvp.core.loot.LootContext;
import me.mykindos.betterpvp.core.loot.ReplacementStrategy;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;
import java.util.function.Predicate;

/**
 * Loot that drops an item at the location of the given {@link LootContext}.
 */
public abstract sealed class ItemLoot<T> extends Loot<BaseItem, T> permits GivenItemLoot, DroppedItemLoot {

    protected final int minAmount;
    protected final int maxAmount;

    ItemLoot(BaseItem reward, ReplacementStrategy replacementStrategy, Predicate<LootContext> condition, int minAmount, int maxAmount) {
        super(reward, replacementStrategy, condition);
        this.minAmount = minAmount;
        this.maxAmount = maxAmount;
    }

    public static DroppedItemLoot dropped(BaseItem reward, int minAmount, int maxAmount) {
        return new DroppedItemLoot(reward, ReplacementStrategy.UNSET, context -> true, minAmount, maxAmount);
    }

    public static DroppedItemLoot dropped(BaseItem reward, ReplacementStrategy replacementStrategy, int minAmount, int maxAmount) {
        return new DroppedItemLoot(reward, replacementStrategy, context -> true, minAmount, maxAmount);
    }

    public static DroppedItemLoot dropped(BaseItem reward, Predicate<LootContext> condition, int minAmount, int maxAmount) {
        return new DroppedItemLoot(reward, ReplacementStrategy.UNSET, condition, minAmount, maxAmount);
    }

    public static DroppedItemLoot dropped(BaseItem reward, ReplacementStrategy replacementStrategy, Predicate<LootContext> condition, int minAmount, int maxAmount) {
        return new DroppedItemLoot(reward, replacementStrategy, condition, minAmount, maxAmount);
    }

    public static GivenItemLoot given(BaseItem reward, int minAmount, int maxAmount) {
        return new GivenItemLoot(reward, ReplacementStrategy.UNSET, context -> true, minAmount, maxAmount);
    }

    public static GivenItemLoot given(BaseItem reward, ReplacementStrategy replacementStrategy, int minAmount, int maxAmount) {
        return new GivenItemLoot(reward, replacementStrategy, context -> true, minAmount, maxAmount);
    }

    public static GivenItemLoot given(BaseItem reward, Predicate<LootContext> condition, int minAmount, int maxAmount) {
        return new GivenItemLoot(reward, ReplacementStrategy.UNSET, condition, minAmount, maxAmount);
    }

    public static GivenItemLoot given(BaseItem reward, ReplacementStrategy replacementStrategy, Predicate<LootContext> condition, int minAmount, int maxAmount) {
        return new GivenItemLoot(reward, replacementStrategy, condition, minAmount, maxAmount);
    }

    @Override
    public ItemView getIcon() {
        final Core plugin = JavaPlugin.getPlugin(Core.class);
        final ItemFactory itemFactory = plugin.getInjector().getInstance(ItemFactory.class);
        final ItemInstance item = itemFactory.create(this.getReward());
        return ItemView.of(item.getView().get());
    }

    @Override
    public String toString() {
        final Core plugin = JavaPlugin.getPlugin(Core.class);
        final ItemFactory itemFactory = plugin.getInjector().getInstance(ItemFactory.class);
        final NamespacedKey key = Objects.requireNonNull(itemFactory.getItemRegistry().getKey(getReward()));
        return getClass().getSimpleName() + "{" +
                "item=" + key +
                '}';
    }
}