package me.mykindos.betterpvp.core.imbuement;

import com.google.common.base.Preconditions;
import lombok.Getter;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.component.impl.socketables.Socketable;
import me.mykindos.betterpvp.core.item.component.impl.socketables.SocketableContainerComponent;
import me.mykindos.betterpvp.core.item.component.impl.socketables.SocketableItem;
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
public class SocketableImbuementRecipe extends ImbuementRecipe {

    private final BaseItem baseItem;
    private final SocketableItem socketableItem;

    /**
     * Creates a new rune imbuement recipe.
     * @param itemFactory The item factory for item operations
     */
    public SocketableImbuementRecipe(@NotNull ItemFactory itemFactory, BaseItem baseItem, SocketableItem socketable) {
        super(itemFactory);
        final Optional<SocketableContainerComponent> containerOpt = baseItem.getComponent(SocketableContainerComponent.class);
        Preconditions.checkArgument(containerOpt.isPresent(), "Base item must have a rune container component");
        Preconditions.checkArgument(socketable.getSocketable().canApply(baseItem), "Rune cannot be applied to base item");
        this.baseItem = baseItem;
        this.socketableItem = socketable;
    }

    @Override
    public @NotNull ImbuementRecipeResult previewResult() {
        ItemInstance item = itemFactory.createPreview(baseItem)
                .withComponent(new SocketableContainerComponent(1, 1));
        ItemInstance rune = itemFactory.createPreview(socketableItem);
        return new ImbuementRecipeResult(applyRuneToItems(List.of(item, rune)));
    }

    @Override
    public @NotNull ImbuementRecipeResult createResult() {
        ItemInstance item = itemFactory.create(baseItem)
                .withComponent(new SocketableContainerComponent(1, 1));
        ItemInstance rune = itemFactory.create(socketableItem);
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
        SocketableData socketableData = extractRuneData(items);
        if (socketableData == null) {
            throw new IllegalArgumentException("Invalid rune recipe items");
        }

        return applyRuneToItem(socketableData.targetItem);
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

        SocketableData socketableData = extractRuneData(items);
        return socketableData != null;
    }

    private SocketableData extractRuneData(@NotNull List<ItemInstance> items) {
        ItemInstance runeItemInstance = null;
        ItemInstance targetItemInstance = null;

        for (ItemInstance item : items) {
            if (item == null) continue;
            ItemStack stack = item.createItemStack();

            if (item.getBaseItem() == socketableItem) {
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

        if (!socketableItem.getSocketable().canApply(targetItemInstance)) {
            return null;
        }

        Optional<SocketableContainerComponent> containerOpt = targetItemInstance.getComponent(SocketableContainerComponent.class);
        if (containerOpt.isEmpty() || !containerOpt.get().hasAvailableSockets()) {
            return null;
        }

        if (containerOpt.get().hasRune(socketableItem.getSocketable())) {
            return null;
        }

        return new SocketableData(socketableItem.getSocketable(), runeItemInstance, targetItemInstance);
    }

    private ItemInstance applyRuneToItem(@NotNull ItemInstance targetItem) {
        Optional<SocketableContainerComponent> containerOpt = targetItem.getComponent(SocketableContainerComponent.class);
        if (containerOpt.isEmpty()) {
            throw new IllegalArgumentException("Target item does not have a rune container component");
        }

        SocketableContainerComponent existing = containerOpt.get();

        if (!existing.hasAvailableSockets()) {
            throw new IllegalArgumentException("Target item's rune container is full");
        }

        List<Socketable> newSocketables = new ArrayList<>(existing.getSocketables());
        newSocketables.add(socketableItem.getSocketable());
        SocketableContainerComponent newContainer = new SocketableContainerComponent(
                existing.getSockets(),
                existing.getMaxSockets(),
                newSocketables
        );

        return targetItem.withComponent(newContainer);
    }

    @Override
    public @NotNull Map<Integer, RecipeIngredient> getIngredients() {
        return Map.of(
                0, new RecipeIngredient(socketableItem, 1),
                1, new RecipeIngredient(baseItem, 1)
        );
    }

    private static class SocketableData {
        final Socketable socketable;
        final ItemInstance socketableItem;
        final ItemInstance targetItem;

        SocketableData(Socketable socketable, ItemInstance socketableItem, ItemInstance targetItem) {
            this.socketable = socketable;
            this.socketableItem = socketableItem;
            this.targetItem = targetItem;
        }
    }
}
