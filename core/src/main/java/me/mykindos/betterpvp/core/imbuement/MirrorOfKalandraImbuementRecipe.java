package me.mykindos.betterpvp.core.imbuement;

import com.google.common.base.Preconditions;
import lombok.Getter;
import me.mykindos.betterpvp.core.framework.blockbreak.component.ToolComponent;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.ItemRegistry;
import me.mykindos.betterpvp.core.item.component.impl.socketables.runes.RuneItem;
import me.mykindos.betterpvp.core.item.component.impl.uuid.UUIDItem;
import me.mykindos.betterpvp.core.item.component.impl.uuid.UUIDManager;
import me.mykindos.betterpvp.core.item.component.impl.uuid.UUIDProperty;
import me.mykindos.betterpvp.core.item.impl.MirrorOfKalandra;
import me.mykindos.betterpvp.core.item.model.ArmorItem;
import me.mykindos.betterpvp.core.item.model.WeaponItem;
import me.mykindos.betterpvp.core.recipe.RecipeIngredient;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Imbuement recipe that duplicates an item by consuming a {@link MirrorOfKalandra}.
 * <p>
 * Matches any unstackable tool, armour piece or weapon paired with a single mirror. Executing
 * the recipe consumes the mirror and yields both the original item and an identical copy —
 * all components carry over, except a {@link UUIDProperty}, which is regenerated (and registered)
 * on the copy so the two items remain distinct and independently tracked.
 */
@Getter
public class MirrorOfKalandraImbuementRecipe extends ImbuementRecipe {

    private final @NotNull BaseItem mirror;
    private final @NotNull UUIDManager uuidManager;
    private final @NotNull ItemRegistry itemRegistry;

    public MirrorOfKalandraImbuementRecipe(@NotNull ItemFactory itemFactory,
                                           @NotNull BaseItem mirror,
                                           @NotNull UUIDManager uuidManager,
                                           @NotNull ItemRegistry itemRegistry) {
        super(itemFactory);
        this.mirror = mirror;
        this.uuidManager = uuidManager;
        this.itemRegistry = itemRegistry;
    }

    /**
     * An item is duplicable if it is an unstackable tool, armour piece or weapon — i.e. a single
     * item that carries durability. This deliberately excludes stackable materials and the mirror.
     */
    public static boolean isDuplicable(@NotNull ItemInstance item) {
        final BaseItem baseItem = item.getBaseItem();
        if (baseItem instanceof MirrorOfKalandra) {
            return false;
        }

        if (!(baseItem instanceof ArmorItem)
                && !(baseItem instanceof WeaponItem)
                && !(baseItem instanceof RuneItem)
                && item.getComponent(ToolComponent.class).isEmpty()) {
            // Not a tool, armour piece, weapon or rune, and has no durability component; not duplicable.
            return false;
        }

        final ItemStack stack = item.createItemStack();
        return stack.getMaxStackSize() == 1;
    }

    @Override
    public @NotNull ImbuementRecipeResult previewResult() {
        return new ImbuementRecipeResult(itemFactory.createPreview(mirror));
    }

    @Override
    public @NotNull ImbuementRecipeResult createResult() {
        // The real output is produced from the placed items via duplicate(); this placeholder
        // is only used by access checks and the recipe browser, so it stays side-effect-free.
        return new ImbuementRecipeResult(itemFactory.createPreview(mirror));
    }

    /**
     * Produces the items yielded by this recipe from the items placed on the pedestal: the original
     * duplicable item unchanged, plus an identical copy with a freshly minted UUID (if applicable).
     *
     * @param placedItems the items currently on the pedestal (one mirror + one duplicable item)
     * @return the original item and its copy, in that order
     */
    public @NotNull List<ItemInstance> duplicate(@NotNull List<ItemInstance> placedItems) {
        final ItemInstance original = findDuplicable(placedItems);
        Preconditions.checkArgument(original != null, "No duplicable item present for mirror recipe");

        ItemInstance copy = itemFactory.fromItemStack(original.createItemStack()).orElseThrow();

        final Optional<UUIDProperty> existingUuid = copy.getComponent(UUIDProperty.class);
        if (existingUuid.isPresent()) {
            final UUIDProperty freshUuid = new UUIDProperty();
            final NamespacedKey key = itemRegistry.getKey(copy.getBaseItem());
            if (key != null) {
                uuidManager.addUuid(new UUIDItem(freshUuid.getUniqueId(), key.getNamespace(), key.getKey()));
            }
            copy = copy.withComponent(freshUuid);
        }

        return List.of(original, copy);
    }

    @Override
    public boolean matches(@NotNull Map<Integer, ItemStack> items) {
        ItemInstance mirrorItem = null;
        ItemInstance duplicableItem = null;

        for (ItemStack stack : items.values()) {
            if (stack == null || stack.getType().isAir()) {
                continue;
            }
            if (stack.getAmount() != 1) {
                return false;
            }

            final Optional<ItemInstance> instanceOpt = itemFactory.fromItemStack(stack);
            if (instanceOpt.isEmpty()) {
                return false;
            }
            final ItemInstance instance = instanceOpt.get();

            if (instance.getBaseItem() instanceof MirrorOfKalandra) {
                if (mirrorItem != null) {
                    return false;
                }
                mirrorItem = instance;
            } else if (isDuplicable(instance)) {
                if (duplicableItem != null) {
                    return false;
                }
                duplicableItem = instance;
            } else {
                return false;
            }
        }

        return mirrorItem != null && duplicableItem != null;
    }

    @Override
    public @NotNull Map<Integer, RecipeIngredient> getIngredients() {
        return Map.of(0, new RecipeIngredient(mirror, 1));
    }

    @Nullable
    private ItemInstance findDuplicable(@NotNull List<ItemInstance> placedItems) {
        final List<ItemInstance> candidates = new ArrayList<>();
        for (ItemInstance item : placedItems) {
            if (item != null && isDuplicable(item)) {
                candidates.add(item);
            }
        }
        return candidates.size() == 1 ? candidates.getFirst() : null;
    }
}
