package me.mykindos.betterpvp.core.loot.menu;

import me.mykindos.betterpvp.core.inventory.gui.AbstractPagedGui;
import me.mykindos.betterpvp.core.inventory.gui.SlotElement;
import me.mykindos.betterpvp.core.inventory.gui.structure.Markers;
import me.mykindos.betterpvp.core.inventory.gui.structure.Structure;
import me.mykindos.betterpvp.core.inventory.item.Item;
import me.mykindos.betterpvp.core.inventory.item.ItemWrapper;
import me.mykindos.betterpvp.core.inventory.item.impl.SimpleItem;
import me.mykindos.betterpvp.core.loot.Loot;
import me.mykindos.betterpvp.core.menu.Menu;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.menu.button.BackButton;
import me.mykindos.betterpvp.core.menu.button.PageBackwardButton;
import me.mykindos.betterpvp.core.menu.button.PageForwardButton;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A menu that displays all loot tables that contain a specific item,
 * showing the item as it appears in each drop table along with its
 * drop chance and the source loot table name.
 */
public class ItemDropTablesMenu extends AbstractPagedGui<Item> implements Windowed {

    /**
     * Represents a single occurrence of an item within a loot table.
     *
     * @param lootTableId The ID of the loot table this item was found in.
     * @param loot        The loot entry that will provide the item icon and amount info.
     * @param chance      The drop chance (0.0–1.0) of this entry in the loot table.
     */
    public record Appearance(String lootTableId, Loot<?, ?> loot, float chance) {}

    /**
     * Creates a new ItemDropTablesMenu.
     *
     * @param appearances All appearances of the item across all loot tables.
     * @param previous    The previous menu to return to, or null if there is none.
     */
    public ItemDropTablesMenu(List<Appearance> appearances, @Nullable Windowed previous) {
        super(9, 6, false, new Structure(
                "# # # # # # # # #",
                "# x x x x x x x #",
                "# x x x x x x x #",
                "# x x x x x x x #",
                "# x x x x x x x #",
                "# # # < - > # # #")
                .addIngredient('x', Markers.CONTENT_LIST_SLOT_HORIZONTAL)
                .addIngredient('#', Menu.BACKGROUND_ITEM)
                .addIngredient('<', new PageBackwardButton())
                .addIngredient('-', new BackButton(previous))
                .addIngredient('>', new PageForwardButton()));

        List<Item> items = new ArrayList<>();

        for (Appearance appearance : appearances) {
            ItemStack itemStack = appearance.loot().getIcon().get();
            double chance = appearance.chance();

            ItemStack displayItem = itemStack.clone();
            ItemMeta meta = displayItem.getItemMeta();
            if (meta != null) {
                List<Component> lore = meta.hasLore()
                        ? new ArrayList<>(Objects.requireNonNull(meta.lore()))
                        : new ArrayList<>();

                // Show the source loot table
                lore.add(Component.empty());
                lore.add(Component.text("Source: ", NamedTextColor.YELLOW)
                        .decoration(TextDecoration.ITALIC, false)
                        .append(Component.text(appearance.lootTableId(), NamedTextColor.WHITE)
                                .decoration(TextDecoration.ITALIC, false)));

                // Show the drop chance
                String percentage = UtilFormat.formatNumber(chance * 100, getDecimalPlaces(chance * 100));
                double oneInX = 1.0 / chance;
                String rarity;
                if (oneInX >= 1_000_000) {
                    rarity = String.format("1 in %.1fM", oneInX / 1_000_000);
                } else if (oneInX >= 1_000) {
                    rarity = String.format("1 in %,d", Math.round(oneInX));
                } else {
                    rarity = String.format("1 in %.1f", oneInX);
                }
                lore.add(Component.text("Drop Chance: ", NamedTextColor.YELLOW)
                        .decoration(TextDecoration.ITALIC, false)
                        .append(Component.text(rarity + " (" + percentage + "%)", NamedTextColor.WHITE)
                                .decoration(TextDecoration.ITALIC, false)));

                meta.lore(lore);
                meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
                displayItem.setItemMeta(meta);
            }

            items.add(new SimpleItem(new ItemWrapper(displayItem)));
        }

        setContent(items);
    }

    @Override
    public @NotNull Component getTitle() {
        return Component.text("Drop Tables");
    }

    @Override
    public void bake() {
        int contentSize = getContentListSlots().length;

        List<List<SlotElement>> pages = new ArrayList<>();
        List<SlotElement> page = new ArrayList<>(contentSize);

        for (Item item : content) {
            page.add(new SlotElement.ItemSlotElement(item));

            if (page.size() >= contentSize) {
                pages.add(page);
                page = new ArrayList<>(contentSize);
            }
        }

        if (!page.isEmpty()) {
            pages.add(page);
        }

        this.pages = pages;
        update();
    }

    private int getDecimalPlaces(double percent) {
        if (percent >= 10) return 0;
        if (percent >= 1) return 1;
        if (percent >= 0.1) return 2;
        if (percent >= 0.01) return 4;
        return 6;
    }
}

