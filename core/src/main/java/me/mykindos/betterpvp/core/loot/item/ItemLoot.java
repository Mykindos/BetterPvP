package me.mykindos.betterpvp.core.loot.item;

import lombok.CustomLog;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.loot.Loot;
import me.mykindos.betterpvp.core.loot.LootContext;
import me.mykindos.betterpvp.core.loot.ReplacementStrategy;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.function.Predicate;

/**
 * Loot that drops an item at the location of the given {@link LootContext}.
 */
@CustomLog
@Getter
@EqualsAndHashCode(callSuper = true)
public abstract sealed class ItemLoot<T> extends Loot<ItemInstance, T> permits GivenItemLoot, DroppedItemLoot {

    protected final NamespacedKey itemKey;
    protected final int minAmount;
    protected final int maxAmount;

    ItemLoot(NamespacedKey itemKey, ReplacementStrategy replacementStrategy, Predicate<LootContext> condition, int minAmount, int maxAmount) {
        super(replacementStrategy, condition);
        this.itemKey = itemKey;
        this.minAmount = minAmount;
        this.maxAmount = maxAmount;
    }

    public static DroppedItemLoot dropped(NamespacedKey itemKey, int minAmount, int maxAmount) {
        return new DroppedItemLoot(itemKey, ReplacementStrategy.UNSET, context -> true, minAmount, maxAmount);
    }

    public static DroppedItemLoot dropped(NamespacedKey itemKey, ReplacementStrategy replacementStrategy, int minAmount, int maxAmount) {
        return new DroppedItemLoot(itemKey, replacementStrategy, context -> true, minAmount, maxAmount);
    }

    public static DroppedItemLoot dropped(NamespacedKey itemKey, Predicate<LootContext> condition, int minAmount, int maxAmount) {
        return new DroppedItemLoot(itemKey, ReplacementStrategy.UNSET, condition, minAmount, maxAmount);
    }

    public static DroppedItemLoot dropped(NamespacedKey itemKey, ReplacementStrategy replacementStrategy, Predicate<LootContext> condition, int minAmount, int maxAmount) {
        return new DroppedItemLoot(itemKey, replacementStrategy, condition, minAmount, maxAmount);
    }

    public static GivenItemLoot given(NamespacedKey itemKey, int minAmount, int maxAmount) {
        return new GivenItemLoot(itemKey, ReplacementStrategy.UNSET, context -> true, minAmount, maxAmount);
    }

    public static GivenItemLoot given(NamespacedKey itemKey, ReplacementStrategy replacementStrategy, int minAmount, int maxAmount) {
        return new GivenItemLoot(itemKey, replacementStrategy, context -> true, minAmount, maxAmount);
    }

    public static GivenItemLoot given(NamespacedKey itemKey, Predicate<LootContext> condition, int minAmount, int maxAmount) {
        return new GivenItemLoot(itemKey, ReplacementStrategy.UNSET, condition, minAmount, maxAmount);
    }

    public static GivenItemLoot given(NamespacedKey itemKey, ReplacementStrategy replacementStrategy, Predicate<LootContext> condition, int minAmount, int maxAmount) {
        return new GivenItemLoot(itemKey, replacementStrategy, condition, minAmount, maxAmount);
    }

    @Override
    public ItemInstance getReward() {
        final Core plugin = JavaPlugin.getPlugin(Core.class);
        final ItemFactory itemFactory = plugin.getInjector().getInstance(ItemFactory.class);
        final BaseItem baseItem = itemFactory.getItemRegistry().getItem(itemKey);
        if (baseItem == null) {
            throw new IllegalArgumentException("Item " + itemKey + " does not exist");
        }
        return itemFactory.create(baseItem);
    }

    @Override
    public ItemView getIcon() {
        ItemStack itemStack = this.getReward().getView().get();
        final ItemView.ItemViewBuilder builder = ItemView.of(itemStack).toBuilder();
        if(minAmount != maxAmount) {
            builder.lore(Component.empty());
            builder.lore(Component.empty()
                    .append(Component.text("Minimum Amount: ", NamedTextColor.GRAY))
                    .append(Component.text(minAmount, NamedTextColor.WHITE)));
            builder.lore(Component.empty()
                    .append(Component.text("Maximum Amount: ", NamedTextColor.GRAY))
                    .append(Component.text(maxAmount, NamedTextColor.WHITE)));
        }
        return builder.build();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "item=" + itemKey +
                '}';
    }
}