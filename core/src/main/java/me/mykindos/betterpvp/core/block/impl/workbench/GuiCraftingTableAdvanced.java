package me.mykindos.betterpvp.core.block.impl.workbench;

import lombok.SneakyThrows;
import me.mykindos.betterpvp.core.inventory.gui.structure.Structure;
import me.mykindos.betterpvp.core.inventory.inventory.VirtualInventory;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.AbstractItem;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.component.impl.blueprint.BlueprintComponent;
import me.mykindos.betterpvp.core.menu.button.InfoTabButton;
import me.mykindos.betterpvp.core.recipe.crafting.CraftingManager;
import me.mykindos.betterpvp.core.recipe.crafting.menu.AbstractCraftingGui;
import me.mykindos.betterpvp.core.utilities.Resources;
import me.mykindos.betterpvp.core.utilities.UtilItem;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class GuiCraftingTableAdvanced extends AbstractCraftingGui {

    private final GuiWorkbench parent;
    private final VirtualInventory blueprintInventory;

    @SneakyThrows
    GuiCraftingTableAdvanced(GuiWorkbench parent, CraftingManager craftingManager, ItemFactory itemFactory) {
        super(craftingManager, itemFactory, 9, 6);
        this.parent = parent;
        this.blueprintInventory = new VirtualInventory(UUID.randomUUID(), new ItemStack[1]);

        // Setup GUI structure with crafting grid, result, quick crafts, and blueprint button
        applyStructure(new Structure(
                "0000000AI",
                "0XXX00000",
                "0XXXB0R00",
                "0XXX00000",
                "000000000",
                "V01234567")
                .addIngredient('X', craftingMatrix)
                .addIngredient('R', resultInventory)
                .addIngredient('1', new QuickCraftingButton(0, parent))
                .addIngredient('2', new QuickCraftingButton(1, parent))
                .addIngredient('3', new QuickCraftingButton(2, parent))
                .addIngredient('4', new QuickCraftingButton(3, parent))
                .addIngredient('5', new QuickCraftingButton(4, parent))
                .addIngredient('6', new QuickCraftingButton(5, parent))
                .addIngredient('7', new QuickCraftingButton(6, parent))
                .addIngredient('V', new QuickCraftViewerButton())
                .addIngredient('B', new BlueprintHolder(itemFactory, blueprintInventory))
                .addIngredient('I', InfoTabButton.builder()
                        .description(Component.text("Use this workbench to craft items using blueprints and ingredients in your inventory."))
                        // todo: wiki entry
                        .wikiEntry("Test", URI.create("https://wiki.betterpvp.net/").toURL())
                        .wikiEntry("Test2", URI.create("https://wiki.betterpvp.net/").toURL())
                        .build()));
    }

    @Override
    protected List<BlueprintComponent> getBlueprints() {
        return Arrays.stream(blueprintInventory.getItems())
                .filter(Objects::nonNull)
                .flatMap(item -> itemFactory.fromItemStack(item).stream())
                .flatMap(instance -> instance.getComponent(BlueprintComponent.class).stream())
                .toList();
    }

    private ItemStack getBlueprintPlaceholder() {
        return ItemView.builder()
                .material(Material.BARRIER)
                .itemModel(Resources.ItemModel.STOP)
                .displayName(Component.text("Add Blueprint", NamedTextColor.GREEN))
                .lore(Component.text("Drag a blueprint from your", NamedTextColor.GRAY))
                .lore(Component.text("inventory into this slot to", NamedTextColor.GRAY))
                .lore(Component.text("use it.", NamedTextColor.GRAY))
                .build()
                .get();
    }

    @Override
    public void refund(Player player) {
        super.refund(player);
        for (ItemStack item : blueprintInventory.getItems()) {
            if (item != null && item.getType() != Material.BARRIER) {
                UtilItem.insert(player, item);
            }
        }
    }

    public VirtualInventory getCraftingMatrix() {
        return craftingMatrix;
    }

    private class QuickCraftViewerButton extends AbstractItem {
        @Override
        public ItemProvider getItemProvider() {
            return ItemView.builder()
                    .material(Material.PAPER)
                    .itemModel(Key.key("betterpvp", "menu/icon/regular/book_icon"))
                    .displayName(Component.text("More Quick Crafts", TextColor.color(0, 207, 41)))
                    .build();
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
            parent.setQuickCraftTab();
        }
    }
}