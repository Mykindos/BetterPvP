package me.mykindos.betterpvp.core.recipe.crafting.menu;

import com.google.inject.Inject;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.inventory.gui.AbstractGui;
import me.mykindos.betterpvp.core.inventory.inventory.VirtualInventory;
import me.mykindos.betterpvp.core.inventory.inventory.event.ItemPreUpdateEvent;
import me.mykindos.betterpvp.core.inventory.inventory.event.PlayerUpdateReason;
import me.mykindos.betterpvp.core.inventory.window.Window;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.component.impl.blueprint.BlueprintComponent;
import me.mykindos.betterpvp.core.recipe.crafting.CraftingRecipe;
import me.mykindos.betterpvp.core.recipe.crafting.CraftingManager;
import me.mykindos.betterpvp.core.recipe.crafting.CraftingResult;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.utilities.Resources;
import me.mykindos.betterpvp.core.utilities.UtilItem;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@CustomLog
public abstract class AbstractCraftingGui extends AbstractGui {

    protected final CraftingManager craftingManager;
    protected final ItemFactory itemFactory;
    protected final VirtualInventory craftingMatrix;
    protected final VirtualInventory resultInventory;
    protected boolean blocked = false;

    @Inject
    protected AbstractCraftingGui(CraftingManager craftingManager, ItemFactory itemFactory) {
        super(9, 6);
        this.craftingManager = craftingManager;
        this.itemFactory = itemFactory;
        this.craftingMatrix = new VirtualInventory(UUID.randomUUID(), new ItemStack[3 * 3]);
        this.resultInventory = new VirtualInventory(UUID.randomUUID(), new ItemStack[1]);

        // Crafting matrix updating
        craftingMatrix.setPostUpdateHandler(event -> {
            Player player = null;
            if (event.getUpdateReason() instanceof PlayerUpdateReason updateReason) {
                player = updateReason.getPlayer();
            }
            final ItemStack newResult = processResult(player, craftingMatrix.getItems());
            if (player != null && newResult != null && !newResult.isSimilar(resultInventory.getItem(0))) {
                playUpdated(player);
            }
            resultInventory.setItemSilently(0, newResult);
        });

        // Item consumption when crafting
        resultInventory.setPreUpdateHandler(event -> {
            Player player = null;

            if (blocked) {
                if (event.getUpdateReason() instanceof PlayerUpdateReason updateReason) {
                    SoundEffect.WRONG_ACTION.play(updateReason.getPlayer());
                }
                event.setCancelled(true);
                return;
            }
            
            if (event.getUpdateReason() instanceof PlayerUpdateReason updateReason) {
                player = updateReason.getPlayer();
                final InventoryEvent invEvent = updateReason.getEvent();
                
                // If they place a new item in the backing inventory, cancel the event
                // If they're taking an item out, allow it
                final ItemStack result = event.getPreviousItem();
                if (result != null) {
                    if (invEvent instanceof InventoryClickEvent clickEvent) {
                        switch (clickEvent.getClick()) {
                            // Shift clicking the result item
                            case SHIFT_LEFT:
                                handleShiftClick(player, result);
                                clickEvent.setCancelled(true);
                                event.setCancelled(true);
                                return;

                            // Merging with cursor item
                            case LEFT:
                                if (event.getNewItem() != null) {
                                    handleCursorMerge(player, result, clickEvent.getCursor(), event, clickEvent);
                                    event.setCancelled(true);
                                    return;
                                }
                                break;

                            // Everything else, cancel
                            default:
                                event.setCancelled(true);
                                return;
                        }
                    }
                }
            }

            // Attempt to craft an item, if it fails, cancel the player clicking the result
            if (!craft(player)) {
                event.setCancelled(true);
                playCrafted(player);
                return;
            }

            // If crafting was successful, update the matrix again because the result may have changed
            event.setNewItem(processResult(player, craftingMatrix.getItems()));
        });
    }

    /**
     * Handles shift-click crafting, crafting as many items as possible and inserting them into the player's inventory.
     */
    protected void handleShiftClick(Player player, ItemStack resultItem) {
        if (player == null || resultItem == null) {
            return;
        }

        ItemStack craftedStack = resultItem.clone();
        ItemStack toInsert = craftedStack.clone();
        int initialAmount = craftedStack.getAmount();
        int crafted = 0;

        // Keep crafting until we can't anymore or inventory is full
        while (player.getInventory().firstEmpty() != -1 || UtilItem.fits(player, toInsert, initialAmount * (crafted + 1))) {
            if (!craft(player)) {
                break;
            }

            // Update the result slot
            resultInventory.setItemSilently(0, processResult(player, craftingMatrix.getItems()));

            // Update the crafted stack
            crafted++;
            toInsert = craftedStack.clone();
            toInsert.setAmount(initialAmount * crafted);

            // Update result for next iteration
            craftedStack = processResult(player, craftingMatrix.getItems());
            if (craftedStack == null || !toInsert.isSimilar(craftedStack)) {
                break; // Recipe changed or no more results
            }
        }

        if (crafted <= 0) {
            // If no items were crafted, do nothing
            SoundEffect.WRONG_ACTION.play(player);
            return;
        }

        UtilItem.insert(player, toInsert);
        playCrafted(player);
    }

    /**
     * Handles merging a crafted item with an item in the player's cursor.
     */
    protected void handleCursorMerge(Player player, ItemStack resultItem, ItemStack cursorItem, ItemPreUpdateEvent updateEvent, InventoryClickEvent clickEvent) {
        if (player == null || resultItem == null || cursorItem == null || clickEvent == null) {
            return;
        }

        if (!resultItem.isSimilar(cursorItem)) {
            // If the cursor item is not the same as the result item, do nothing
            return;
        }
        
        int maxStackSize = cursorItem.getMaxStackSize();
        int cursorAmount = cursorItem.getAmount();
        int resultAmount = resultItem.getAmount();
        int totalAmount = cursorAmount + resultAmount;

        // If the combined amount exceeds max stack size
        if (totalAmount > maxStackSize) {
            updateEvent.setCancelled(true);
            SoundEffect.WRONG_ACTION.play(player);
            return;
        }

        // Craft the item and update the result slot
        if (craft(player)) {
            cursorItem.setAmount(totalAmount);
            updateEvent.setCancelled(true);
            clickEvent.setCursor(cursorItem);
            updateEvent.setNewItem(ItemStack.of(Material.AIR));
            resultInventory.setItemSilently(0, processResult(player, craftingMatrix.getItems()));
            playCrafted(player);
        }
    }

    /**
     * Converts an array of ItemStacks to a map of ItemInstances.
     */
    protected Map<Integer, ItemInstance> getItemInstanceMatrix(@Nullable ItemStack[] items) {
        if (items == null) {
            return Collections.emptyMap();
        }
        
        Map<Integer, ItemInstance> craftingMatrix = new HashMap<>();
        for (int i = 0; i < items.length; i++) {
            final int slotIndex = i;
            final ItemStack item = items[i];
            if (item != null && !item.getType().isAir()) {
                itemFactory.fromItemStack(item).ifPresent(instance -> craftingMatrix.put(slotIndex, instance));
            }
        }
        return craftingMatrix;
    }

    /**
     * Attempts to craft an item using the current crafting matrix.
     */
    protected boolean craft(@Nullable Player player) {
        if (blocked) {
            if (player != null) {
                SoundEffect.LOW_PITCH_PLING.play(player);
            }
            return false; // Crafting is blocked, do not proceed
        }

        Map<Integer, ItemInstance> itemInstanceMatrix = getItemInstanceMatrix(craftingMatrix.getItems());
        
        final CraftingResult result = craftingManager.craftItem(player, itemInstanceMatrix);
        if (result == null) {
            return false;
        }

        if (!result.additionalResults().isEmpty()) {
            log.warn("Crafting result contains additional items, which are not supported in this GUI.").submit();
            return false;
        }

        final Map<Integer, ItemInstance> newMatrix = result.newCraftingMatrix();
        for (int i = 0; i < craftingMatrix.getSize(); i++) {
            ItemInstance instance = newMatrix.get(i);
            craftingMatrix.setItemSilently(i, instance != null ? instance.createItemStack() : null);
        }
        return true;
    }

    /**
     * Updates the result slot based on the current crafting matrix.
     */
    protected ItemStack processResult(@Nullable Player player, @Nullable ItemStack[] items) {
        if (player == null || items == null) {
            return null;
        }

        Map<Integer, ItemInstance> itemInstanceMatrix = getItemInstanceMatrix(items);
        CraftingRecipe result = craftingManager.updateCraftingResult(player, itemInstanceMatrix);
        if (result == null) {
            return null; // No matching recipe found
        }

        if (!result.canCraft(player)) {
            blocked = true;
            return ItemView.builder()
                    .material(Material.BARRIER)
                    .displayName(Component.text("You cannot craft this!", NamedTextColor.RED))
                    .lore(Component.text("This recipe cannot be crafted by you.", NamedTextColor.GRAY))
                    .build()
                    .get();
        }

        if (needsBlueprint(result)) {
            blocked = true;
            return ItemView.builder()
                    .material(Material.BARRIER)
                    .itemModel(Resources.ItemModel.STOP)
                    .displayName(Component.text("You need a blueprint to craft this!", NamedTextColor.RED))
                    .lore(Component.text("This recipe requires a blueprint to be crafted.", NamedTextColor.GRAY))
                    .build()
                    .get();
        }

        blocked = false;
        return result.createPrimaryResult().createItemStack();
    }

    private void playCrafted(Player player) {
        new SoundEffect(Sound.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, 1f, 0.4f).play(player.getLocation());
        new SoundEffect(Sound.BLOCK_ANVIL_USE, 2f, 0.1f).play(player.getLocation());
        new SoundEffect(Sound.ITEM_SPYGLASS_USE, 2f, 0.4f).play(player.getLocation());
        new SoundEffect(Sound.BLOCK_GRINDSTONE_USE, 1.6f, 0.2f).play(player.getLocation());
    }

    private void playUpdated(Player player) {
        if (blocked) return;
        new SoundEffect(Sound.UI_HUD_BUBBLE_POP, 2, 1).play(player);
    }

    private boolean needsBlueprint(@NotNull CraftingRecipe craftingRecipe) {
        return craftingRecipe.needsBlueprint() && getBlueprints().stream().noneMatch(blueprint -> blueprint.getCraftingRecipes().contains(craftingRecipe));
    }

    public void refund(Player player) {
        for (ItemStack item : craftingMatrix.getItems()) {
            UtilItem.insert(player, item);
        }
    }

    protected List<BlueprintComponent> getBlueprints() {
        return Collections.emptyList();
    }
} 