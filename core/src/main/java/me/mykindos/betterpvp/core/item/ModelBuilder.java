package me.mykindos.betterpvp.core.item;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Consumable;
import io.papermc.paper.datacomponent.item.CustomModelData;
import io.papermc.paper.datacomponent.item.TooltipDisplay;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.CustomModelDataComponent;
import org.jetbrains.annotations.NotNull;

/**
 * A flexible builder for creating ItemStacks with various data components.
 * This builder provides a fluent API for setting different properties without
 * requiring multiple method overloads.
 */
@SuppressWarnings("UnstableApiUsage")
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class ModelBuilder {

    @Getter
    private final Material material;
    private final ItemStack itemStack;
    
    private Key itemModel;
    private Integer maxStackSize;
    private Boolean hideTooltip;
    private Integer customModelData;
    private Component displayName;
    private Boolean consumable;
    private Boolean unbreakable;

    /**
     * Sets the custom model data for the item.
     *
     * @param customModelData The custom model data value
     * @return This builder for chaining
     */
    public ModelBuilder customModelData(int customModelData) {
        this.customModelData = customModelData;
        return this;
    }

    /**
     * Sets the item model using a namespace and key.
     *
     * @param namespace The namespace for the model
     * @param key The key for the model
     * @return This builder for chaining
     */
    public ModelBuilder model(@NotNull String namespace, @NotNull String key) {
        this.itemModel = Key.key(namespace, key);
        return this;
    }

    /**
     * Sets the item model using the "betterpvp" namespace.
     *
     * @param model The model key
     * @return This builder for chaining
     */
    public ModelBuilder model(@NotNull String model) {
        return model("betterpvp", model);
    }

    /**
     * Sets the maximum stack size for the item.
     *
     * @param maxStackSize The maximum stack size
     * @return This builder for chaining
     */
    public ModelBuilder maxStackSize(int maxStackSize) {
        this.maxStackSize = maxStackSize;
        return this;
    }

    /**
     * Hides the tooltip for the item.
     *
     * @return This builder for chaining
     */
    public ModelBuilder hideTooltip() {
        this.hideTooltip = true;
        return this;
    }

    /**
     * Sets the display name for the item.
     *
     * @param displayName The display name
     * @return This builder for chaining
     */
    public ModelBuilder displayName(@NotNull Component displayName) {
        this.displayName = displayName;
        return this;
    }

    /**
     * Makes the item consumable.
     *
     * @return This builder for chaining
     */
    public ModelBuilder consumable() {
        this.consumable = true;
        return this;
    }

    /**
     * Makes the item unbreakable.
     *
     * @return This builder for chaining
     */
    public ModelBuilder unbreakable() {
        this.unbreakable = true;
        return this;
    }

    /**
     * Builds the ItemStack with all configured properties.
     *
     * @return The built ItemStack
     */
    public ItemStack build() {
        ItemStack result = itemStack.clone();
        ItemMeta meta = result.getItemMeta();

        // Set custom model data if specified
        if (customModelData != null) {
            result.setData(DataComponentTypes.CUSTOM_MODEL_DATA, CustomModelData.customModelData().addFloat(customModelData).build());
        }

        // Set display name if specified
        if (displayName != null) {
            meta.displayName(displayName);
        }

        // Set unbreakable if specified
        if (unbreakable != null && unbreakable) {
            meta.setUnbreakable(true);
        }

        result.setItemMeta(meta);

        // Set data components
        if (itemModel != null) {
            result.setData(DataComponentTypes.ITEM_MODEL, itemModel);
        }

        if (maxStackSize != null) {
            result.setData(DataComponentTypes.MAX_STACK_SIZE, maxStackSize);
        } else {
            result.unsetData(DataComponentTypes.MAX_STACK_SIZE);
        }

        if (hideTooltip != null && hideTooltip) {
            result.setData(DataComponentTypes.TOOLTIP_DISPLAY, TooltipDisplay.tooltipDisplay().hideTooltip(true).build());
        }

        if (consumable != null && consumable) {
            result.setData(DataComponentTypes.CONSUMABLE, Consumable.consumable());
        } else {
            result.unsetData(DataComponentTypes.CONSUMABLE);
        }

        return result;
    }
} 