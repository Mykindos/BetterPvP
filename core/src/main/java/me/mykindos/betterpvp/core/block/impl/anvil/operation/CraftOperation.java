package me.mykindos.betterpvp.core.block.impl.anvil.operation;

import lombok.RequiredArgsConstructor;
import me.mykindos.betterpvp.core.anvil.AnvilRecipe;
import me.mykindos.betterpvp.core.anvil.AnvilRecipeResult;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.client.stats.impl.core.item.ItemStat;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.utilities.model.ProgressColor;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Anvil operation that crafts an {@link AnvilRecipe}: consumes the recipe ingredients
 * and drops the primary and secondary results. This is the original anvil behaviour,
 * extracted behind {@link AnvilOperation}.
 */
@RequiredArgsConstructor
public class CraftOperation implements AnvilOperation {

    private final AnvilRecipe recipe;
    private final ItemFactory itemFactory;
    private final ClientManager clientManager;

    @Override
    public int requiredSwings() {
        return recipe.getRequiredHammerSwings();
    }

    @Override
    public @NotNull Component hologramText(int currentSwings) {
        final int required = requiredSwings();
        final int remaining = required - currentSwings;
        if (remaining <= 0) {
            return Component.empty();
        }
        final TextColor color = ProgressColor.of((float) currentSwings / required).getTextColor();
        return Component.text(remaining, color);
    }

    @Override
    public @NotNull List<ItemInstance> complete(@NotNull Player player,
                                                @NotNull Map<Integer, ItemInstance> items,
                                                @NotNull Location location) {
        // Consume ingredients
        recipe.consumeIngredients(items, itemFactory);

        // Get remaining items after consumption
        final List<ItemInstance> remainingItems = items.values().stream()
                .filter(Objects::nonNull)
                .toList();

        // Get the live recipe result
        final AnvilRecipeResult result = recipe.createResult();

        // Drop the primary result
        final ItemStack primaryResult = result.getPrimaryResult().createItemStack();
        location.getWorld().dropItemNaturally(location, primaryResult);

        final ItemStat primaryStat = ItemStat.builder()
                .itemStack(primaryResult)
                .action(ItemStat.Action.ANVIL_PRIMARY)
                .build();
        clientManager.incrementStat(player, primaryStat, 1L);

        // Drop secondary results
        for (BaseItem secondaryResult : result.getSecondaryResults()) {
            final ItemStack secondaryStack = itemFactory.create(secondaryResult).createItemStack();
            location.getWorld().dropItemNaturally(location, secondaryStack);
            final ItemStat secondaryStat = ItemStat.builder()
                    .itemStack(secondaryStack)
                    .action(ItemStat.Action.ANVIL_SECONDARY)
                    .build();
            clientManager.incrementStat(player, secondaryStat, 1L);
        }

        // Play completion effects
        new SoundEffect(Sound.BLOCK_ANVIL_LAND, 1.0f, 1.2f).play(location);

        return remainingItems;
    }
}
