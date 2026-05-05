package me.mykindos.betterpvp.core.imbuement;

import com.google.common.base.Preconditions;
import lombok.Getter;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.component.impl.runes.Rune;
import me.mykindos.betterpvp.core.item.component.impl.runes.RuneContainerComponent;
import me.mykindos.betterpvp.core.item.component.impl.runes.RuneItem;
import me.mykindos.betterpvp.core.recipe.RecipeIngredient;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Specialized imbuement recipe for applying runes to items.
 * Dynamically matches any rune + compatible item combination.
 */
@Getter
public class RuneImbuementRecipe extends ImbuementRecipe {

    private final BaseItem baseItem;
    private final RuneItem runeItem;

    /**
     * Creates a new rune imbuement recipe.
     * @param itemFactory The item factory for item operations
     */
    public RuneImbuementRecipe(@NotNull ItemFactory itemFactory, BaseItem baseItem, RuneItem rune) {
        super(itemFactory);
        final Optional<RuneContainerComponent> containerOpt = baseItem.getComponent(RuneContainerComponent.class);
        Preconditions.checkArgument(containerOpt.isPresent(), "Base item must have a rune container component");
        Preconditions.checkArgument(rune.getRune().canApply(baseItem), "Rune cannot be applied to base item");
        this.baseItem = baseItem;
        this.runeItem = rune;
    }

    @Override
    public @NotNull ImbuementRecipeResult previewResult() {
        ItemInstance item = itemFactory.createPreview(baseItem)
                .withComponent(new RuneContainerComponent(1, 1));
        ItemInstance rune = itemFactory.createPreview(runeItem);
        return new ImbuementRecipeResult(applyRuneToItems(List.of(item, rune)));
    }

    @Override
    public @NotNull ImbuementRecipeResult createResult() {
        ItemInstance item = itemFactory.create(baseItem)
                .withComponent(new RuneContainerComponent(1, 1));
        ItemInstance rune = itemFactory.create(runeItem);
        return new ImbuementRecipeResult(applyRuneToItems(List.of(item, rune)));
    }

    /**
     * Applies the rune to the given items and returns the resulting target item.
     * Used by the imbuement pedestal executor to materialize the actual crafted result
     * based on the specific input items provided.
     *
     * @param items The items used in the recipe (should contain 1 rune + 1 target)
     * @return The target item with the rune applied
     */
    public @NotNull ItemInstance applyRuneToItems(@NotNull List<ItemInstance> items) {
        RuneData runeData = extractRuneData(items);
        if (runeData == null) {
            throw new IllegalArgumentException("Invalid rune recipe items");
        }

        return applyRuneToItem(runeData.targetItem);
    }

    @Override
    public boolean matches(@NotNull Map<Integer, ItemStack> items) {
        List<ItemInstance> itemInstances = new ArrayList<>();
        for (ItemStack stack : items.values()) {
            if (stack == null || stack.getType().isAir()) {
                continue;
            }

            itemFactory.fromItemStack(stack).ifPresent(itemInstances::add);
        }

        return isValidRuneRecipe(itemInstances);
    }

    private boolean isValidRuneRecipe(@NotNull List<ItemInstance> items) {
        if (items.size() != 2) {
            return false;
        }

        RuneData runeData = extractRuneData(items);
        return runeData != null;
    }

    private RuneData extractRuneData(@NotNull List<ItemInstance> items) {
        ItemInstance runeItemInstance = null;
        ItemInstance targetItemInstance = null;

        for (ItemInstance item : items) {
            if (item == null) continue;
            ItemStack stack = item.createItemStack();

            if (item.getBaseItem() == runeItem) {
                if (runeItemInstance != null) {
                    return null;
                }
                if (stack.getAmount() != 1) {
                    return null;
                }
                runeItemInstance = item;
            } else if (item.getBaseItem() == baseItem) {
                if (targetItemInstance != null) {
                    return null;
                }
                if (stack.getAmount() != 1) {
                    return null;
                }
                targetItemInstance = item;
            }
        }

        if (runeItemInstance == null || targetItemInstance == null) {
            return null;
        }

        if (!runeItem.getRune().canApply(targetItemInstance)) {
            return null;
        }

        Optional<RuneContainerComponent> containerOpt = targetItemInstance.getComponent(RuneContainerComponent.class);
        if (containerOpt.isEmpty() || !containerOpt.get().hasAvailableSockets()) {
            return null;
        }

        if (containerOpt.get().hasRune(runeItem.getRune())) {
            return null;
        }

        return new RuneData(runeItem.getRune(), runeItemInstance, targetItemInstance);
    }

    private ItemInstance applyRuneToItem(@NotNull ItemInstance targetItem) {
        Optional<RuneContainerComponent> containerOpt = targetItem.getComponent(RuneContainerComponent.class);
        if (containerOpt.isEmpty()) {
            throw new IllegalArgumentException("Target item does not have a rune container component");
        }

        RuneContainerComponent existing = containerOpt.get();

        if (!existing.hasAvailableSockets()) {
            throw new IllegalArgumentException("Target item's rune container is full");
        }

        List<Rune> newRunes = new ArrayList<>(existing.getRunes());
        newRunes.add(runeItem.getRune());
        RuneContainerComponent newContainer = new RuneContainerComponent(
                existing.getSockets(),
                existing.getMaxSockets(),
                newRunes
        );

        return targetItem.withComponent(newContainer);
    }

    @Override
    public @NotNull Map<Integer, RecipeIngredient> getIngredients() {
        return Map.of(
                0, new RecipeIngredient(runeItem, 1),
                1, new RecipeIngredient(baseItem, 1)
        );
    }

    private static class RuneData {
        final Rune rune;
        final ItemInstance runeItem;
        final ItemInstance targetItem;

        RuneData(Rune rune, ItemInstance runeItem, ItemInstance targetItem) {
            this.rune = rune;
            this.runeItem = runeItem;
            this.targetItem = targetItem;
        }
    }
}
