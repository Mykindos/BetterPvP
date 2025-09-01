package me.mykindos.betterpvp.core.droptables;

import lombok.Getter;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.inventory.gui.AbstractPagedGui;
import me.mykindos.betterpvp.core.inventory.gui.SlotElement;
import me.mykindos.betterpvp.core.inventory.gui.structure.Markers;
import me.mykindos.betterpvp.core.inventory.gui.structure.Structure;
import me.mykindos.betterpvp.core.inventory.item.Item;
import me.mykindos.betterpvp.core.inventory.item.ItemWrapper;
import me.mykindos.betterpvp.core.inventory.item.impl.SimpleItem;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.menu.Menu;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.menu.button.BackButton;
import me.mykindos.betterpvp.core.menu.button.ForwardButton;
import me.mykindos.betterpvp.core.menu.button.PreviousButton;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.model.WeighedList;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A specialized WeighedList for ItemStacks that can generate a virtual paged inventory
 * to display all items and their drop chances.
 */
public class DropTable extends WeighedList<DropTableItemStack> {

    @Getter
    private final String name;

    public static final HashMap<String, DropTable> dropTableRegistry = new HashMap<>();

    /**
     * Creates a new DropTable with the given name.
     *
     * @param name The name of the drop table
     */
    public DropTable(String source, String name) {
        super();
        this.name = name;
        dropTableRegistry.putIfAbsent(source + ":" + name, this);
    }

    /**
     * Creates a new DropTable with the given name, copying the contents from another WeighedList.
     *
     * @param name  The name of the drop table
     * @param other The WeighedList to copy from
     */
    public DropTable(String source, String name, WeighedList<DropTableItemStack> other) {
        super(other);
        this.name = name;
        dropTableRegistry.putIfAbsent(source + ":" + name, this);
    }

    /**
     * Shows a virtual paged inventory with all items on the drop table and their drop chances.
     *
     * @param player The player to show the inventory to
     * @return The window that was opened
     */
    public DropTableMenu showInventory(Player player) {
        DropTableMenu menu = new DropTableMenu(this, null);
        menu.show(player);
        return menu;
    }




    /**
     * A menu that displays all items in a drop table along with their drop chances.
     */
    public static class DropTableMenu extends AbstractPagedGui<Item> implements Windowed {
        private final DropTable dropTable;

        /**
         * Creates a new DropTableMenu for the given drop table.
         *
         * @param dropTable The drop table to display
         * @param previous  The previous menu, or null if there is none
         */
        public DropTableMenu(DropTable dropTable, @Nullable Windowed previous) {
            super(9, 6, false, new Structure(
                    "# # # # # # # # #",
                    "# x x x x x x x #",
                    "# x x x x x x x #",
                    "# x x x x x x x #",
                    "# x x x x x x x #",
                    "# # # < - > # # #")
                    .addIngredient('x', Markers.CONTENT_LIST_SLOT_HORIZONTAL)
                    .addIngredient('#', Menu.BACKGROUND_ITEM)
                    .addIngredient('<', new PreviousButton())
                    .addIngredient('-', new BackButton(previous))
                    .addIngredient('>', new ForwardButton()));

            this.dropTable = dropTable;

            // Create items for the GUI
            List<Item> items = new ArrayList<>();
            Map<DropTableItemStack, Float> chances = dropTable.getAbsoluteElementChances();
            // Convert the chances map to a list of entries for sorting
            List<Map.Entry<DropTableItemStack, Float>> sortedItems = new ArrayList<>(chances.entrySet());

            // Sort items by chance in descending order (highest chance = most common first)
            sortedItems.sort(Map.Entry.<DropTableItemStack, Float>comparingByValue().reversed());

            Core core = JavaPlugin.getPlugin(Core.class);
            ItemFactory itemFactory = core.getInjector().getInstance(ItemFactory.class);

            // Add the sorted items to the inventory
            sortedItems.forEach(entry -> {
                if(entry.getKey() == null) {
                    return;
                }

                ItemStack itemStack = itemFactory.convertItemStack(entry.getKey().clone()).orElse(null);
                double chance = entry.getValue();

                if (itemStack == null) return;
                ItemStack displayItem = itemStack.clone();
                ItemMeta meta = displayItem.getItemMeta();
                if (meta != null) {
                    List<Component> lore = meta.hasLore() ? new ArrayList<>(Objects.requireNonNull(meta.lore())) : new ArrayList<>();
                    lore.add(Component.empty());
                    String percentage = UtilFormat.formatNumber(chance * 100, getDecimalPlaces(chance * 100));

                    // Calculate 1 in X with better precision
                    double oneInX = 1.0 / chance;
                    String rarity;
                    if (oneInX >= 1000000) {
                        rarity = String.format("1 in %.1fM", oneInX / 1000000);
                    } else if (oneInX >= 1000) {
                        rarity = String.format("1 in %,d", Math.round(oneInX));
                    } else {
                        rarity = String.format("1 in %.1f", oneInX);
                    }

                    lore.add(Component.text("Drop Chance: ", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false).append(Component.text(rarity + " (" + percentage + "%)", NamedTextColor.WHITE)));

                    meta.lore(lore);
                    meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
                    displayItem.setItemMeta(meta);
                }

                Item item = new SimpleItem(new ItemWrapper(displayItem));
                items.add(item);
            });


            setContent(items);
        }

        @Override
        public @NotNull Component getTitle() {
            return Component.text("Drop Table");
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
            if (percent >= 10) return 0;      // 57%
            if (percent >= 1) return 1;       // 5.1%
            if (percent >= 0.1) return 2;     // 0.92%
            if (percent >= 0.01) return 4;    // 0.0479%
            return 6;                         // 0.000575%
        }
    }
}
