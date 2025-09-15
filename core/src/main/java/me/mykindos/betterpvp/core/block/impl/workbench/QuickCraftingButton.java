package me.mykindos.betterpvp.core.block.impl.workbench;

import me.mykindos.betterpvp.core.inventory.gui.Gui;
import me.mykindos.betterpvp.core.inventory.inventory.VirtualInventory;
import me.mykindos.betterpvp.core.inventory.inventory.event.PlayerUpdateReason;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.controlitem.ControlItem;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.recipe.RecipeIngredient;
import me.mykindos.betterpvp.core.recipe.crafting.CraftingRecipe;
import me.mykindos.betterpvp.core.utilities.Resources;
import me.mykindos.betterpvp.core.utilities.UtilItem;
import me.mykindos.betterpvp.core.utilities.model.item.ClickActions;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedList;
import java.util.Map;

public class QuickCraftingButton extends ControlItem<Gui> {

    private final int slot;
    private final GuiWorkbench workbenchGui;

    public QuickCraftingButton(int slot, GuiWorkbench parent) {
        this.slot = slot;
        this.workbenchGui = parent;
    }

    @Override
    public ItemProvider getItemProvider(Gui gui) {
        LinkedList<CraftingRecipe> quickCrafts = workbenchGui.quickCrafts;
        if (slot < quickCrafts.size()) {
            CraftingRecipe recipe = quickCrafts.get(slot);
            return ItemView.of(recipe.createPrimaryResult().getView().get())
                    .toBuilder()
                    .action(ClickActions.LEFT, Component.text("Quick Craft", TextColor.color(0, 255, 30)))
                    .action(ClickActions.LEFT_SHIFT, Component.text("Bulk Quick Craft", TextColor.color(255, 215, 0)))
                    .build();
        } else {
            return ItemView.builder()
                    .material(Material.BARRIER)
                    .itemModel(Resources.ItemModel.STOP)
                    .displayName(Component.text("No Quick Craft", TextColor.color(255, 0, 0)))
                    .build();
        }
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        LinkedList<CraftingRecipe> quickCrafts = workbenchGui.quickCrafts;
        
        // No recipe
        if (slot >= quickCrafts.size()) {
            return;
        }

        // Invalid click
        if (clickType != ClickType.LEFT && clickType != ClickType.RIGHT && clickType != ClickType.SHIFT_LEFT && clickType != ClickType.SHIFT_RIGHT) {
            return;
        }

        // Refund the player the items in the matrix
        final VirtualInventory craftingMatrix = workbenchGui.craftingGui.getCraftingMatrix();
        final @Nullable ItemStack[] items =
                craftingMatrix.getItems();
        for (int i = 0; i < items.length; i++) {
            ItemStack item = items[i];
            if (item != null) {
                UtilItem.insert(player, item);
            }
            craftingMatrix.setItemSilently(i, null);
        }

        final CraftingRecipe recipe = quickCrafts.get(slot);
        final @Nullable ItemStack[] contents = player.getInventory().getStorageContents();

        // Determine if this is a bulk craft (shift-click)
        final boolean isBulkCraft = clickType == ClickType.SHIFT_LEFT || clickType == ClickType.SHIFT_RIGHT;

        if (isBulkCraft) {
            // Calculate how many times we can craft this recipe
            final int maxCraftable = workbenchGui.lookupParameter.getMaxCraftableAmount(recipe, contents);
            if (maxCraftable <= 0) {
                return; // Can't craft any
            }

            // Remove items for bulk crafting
            if (!workbenchGui.lookupParameter.removeMatchingBulk(recipe, contents, maxCraftable)) {
                return; // Failed to remove items
            }

            player.getInventory().setStorageContents(contents);

            // Place the ingredients for the recipe in the matrix
            for (Map.Entry<Integer, RecipeIngredient> entry : recipe.getIngredients().entrySet()) {
                final Integer matrixSlot = entry.getKey();
                final RecipeIngredient ingredient = entry.getValue();
                final ItemInstance itemInstance = workbenchGui.itemFactory.create(ingredient.getBaseItem());

                // Calculate the total amount to place (ingredient amount * number of crafts possible)
                final int totalAmount = ingredient.getAmount() * maxCraftable;

                // Get the max stack size for this specific item (respects DataComponentTypes.MAX_STACK_SIZE)
                final int maxStackSize = itemInstance.getItemStack().getMaxStackSize();

                // Cap at the item's max stack size to prevent invalid stack sizes
                itemInstance.getItemStack().setAmount(Math.min(totalAmount, maxStackSize));

                final PlayerUpdateReason reason = new PlayerUpdateReason(player, event);
                craftingMatrix.setItem(reason, matrixSlot, itemInstance.getItemStack());
            }
        } else {
            // Regular single craft
            if (!workbenchGui.lookupParameter.removeMatching(recipe, contents)) {
                return; // No matching items found, don't execute the recipe
            }

            player.getInventory().setStorageContents(contents);

            // Place the ingredients for the recipe in the matrix
            for (Map.Entry<Integer, RecipeIngredient> entry : recipe.getIngredients().entrySet()) {
                final Integer matrixSlot = entry.getKey();
                final RecipeIngredient ingredient = entry.getValue();
                final ItemInstance itemInstance = workbenchGui.itemFactory.create(ingredient.getBaseItem());
                itemInstance.getItemStack().setAmount(ingredient.getAmount());
                craftingMatrix.setItemSilently(matrixSlot, itemInstance.getItemStack());
            }

            // Call the update
            final PlayerUpdateReason reason = new PlayerUpdateReason(player, event);
            craftingMatrix.setItemAmount(reason, 0, craftingMatrix.getItemAmount(0));
        }

        // Set the current tab to the crafter, independent of where we are
        workbenchGui.setCraftingTab();
    }
}
