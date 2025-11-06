package me.mykindos.betterpvp.core.item;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.TooltipDisplay;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.item.renderer.ItemLoreRenderer;
import me.mykindos.betterpvp.core.item.renderer.ItemStackRenderer;
import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Represents a view-only version of an {@link ItemInstance}.
 * This item will not work with the item system, but can be used to display the item in an inventory
 * or as a placeholder.
 */
@SuppressWarnings("UnstableApiUsage")
public class ItemInstanceView implements ItemProvider {

    private static final AttributeModifier DUMMY_ATTR = new AttributeModifier(NamespacedKey.minecraft("foo"),
            0,
            AttributeModifier.Operation.MULTIPLY_SCALAR_1);

    private final ItemInstance itemInstance;

    ItemInstanceView(ItemInstance itemInstance) {
        this.itemInstance = itemInstance;
    }

    public Component getName() {
        return itemInstance.getBaseItem().getItemNameRenderer().createName(itemInstance);
    }

    @Override
    public @NotNull ItemStack get(@Nullable String lang) {
        // todo: localization support
        final ItemStack itemStack = itemInstance.createItemStack();

        // Set name and attributes
        itemStack.setData(DataComponentTypes.ITEM_NAME, getName());

//        // Tooltip
        TooltipDisplay.Builder tooltipBuilder = TooltipDisplay.tooltipDisplay();
        if (itemStack.hasData(DataComponentTypes.TOOLTIP_DISPLAY)) {
            final TooltipDisplay existing = Objects.requireNonNull(itemStack.getData(DataComponentTypes.TOOLTIP_DISPLAY));
            tooltipBuilder = TooltipDisplay.tooltipDisplay()
                    .hiddenComponents(existing.hiddenComponents())
                    .hideTooltip(existing.hideTooltip());
        }
        tooltipBuilder.addHiddenComponents(DataComponentTypes.EQUIPPABLE,
                DataComponentTypes.ENCHANTMENTS,
                DataComponentTypes.BANNER_PATTERNS,
                DataComponentTypes.INSTRUMENT,
                DataComponentTypes.BASE_COLOR,
                DataComponentTypes.UNBREAKABLE,
                DataComponentTypes.CAN_BREAK,
                DataComponentTypes.CAN_PLACE_ON,
                DataComponentTypes.ATTRIBUTE_MODIFIERS,
                DataComponentTypes.CHARGED_PROJECTILES
        );
        itemStack.setData(DataComponentTypes.TOOLTIP_DISPLAY, tooltipBuilder.build());

        // lol bug so we can hide attributes
        final ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return itemStack;
        for (EquipmentSlot value : EquipmentSlot.values()) {
            meta.removeAttributeModifier(value);
        }
        meta.addAttributeModifier(Attribute.ATTACK_DAMAGE, DUMMY_ATTR); // This is necessary as of 1.20.6
        itemStack.setItemMeta(meta);
        // end lol bug

        itemStack.addItemFlags(ItemFlag.HIDE_ARMOR_TRIM,
                ItemFlag.HIDE_ATTRIBUTES,
                ItemFlag.HIDE_UNBREAKABLE);

        // Clear pdc
        itemStack.editPersistentDataContainer(pdc -> {
            for (NamespacedKey key : pdc.getKeys()) {
                pdc.remove(key);
            }
        });

        // if lore is available, write it
        final ItemLoreRenderer loreRenderer = itemInstance.getLoreRenderer();
        if (loreRenderer != null) {
            loreRenderer.write(itemInstance, itemStack);
        }

        // Write all item stack renderers
        for (ItemStackRenderer renderer : itemInstance.getBaseItem().getItemStackRenderers()) {
            renderer.write(itemInstance, itemStack);
        }
        return itemStack;
    }
}
