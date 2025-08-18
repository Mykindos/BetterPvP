package me.mykindos.betterpvp.core.block.impl.smelter;

import lombok.CustomLog;
import me.mykindos.betterpvp.core.inventory.gui.AbstractGui;
import me.mykindos.betterpvp.core.inventory.gui.AbstractScrollGui;
import me.mykindos.betterpvp.core.inventory.gui.SlotElement;
import me.mykindos.betterpvp.core.inventory.gui.structure.Markers;
import me.mykindos.betterpvp.core.inventory.gui.structure.Structure;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.controlitem.ControlItem;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.menu.impl.GuiSelectOne;
import me.mykindos.betterpvp.core.metal.casting.CastingMold;
import me.mykindos.betterpvp.core.metal.casting.CastingMoldRecipe;
import me.mykindos.betterpvp.core.metal.casting.CastingMoldRecipeRegistry;
import me.mykindos.betterpvp.core.utilities.Resources;
import me.mykindos.betterpvp.core.utilities.UtilItem;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.utilities.model.item.ClickActions;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static me.mykindos.betterpvp.core.utilities.Resources.Font.NEXO;

@CustomLog
public class GuiCastingMoldPicker extends AbstractGui implements Windowed {

    private final SmelterData smelterData;
    private final ItemFactory itemFactory;
    private final CastingMoldRecipeRegistry recipeRegistry;

    private final GuiDefaultMolds defaultMoldsGui;
    private final GuiStoredMolds storedMoldsGui;

    public GuiCastingMoldPicker(@NotNull SmelterData smelterData, @NotNull ItemFactory itemFactory,
                                @NotNull CastingMoldRecipeRegistry recipeRegistry) {
        super(9, 6);
        this.smelterData = smelterData;
        this.itemFactory = itemFactory;
        this.recipeRegistry = recipeRegistry;

        this.defaultMoldsGui = new GuiDefaultMolds();
        this.storedMoldsGui = new GuiStoredMolds();

        // Set up the layout manually since we can't use nested GUIs in structures
        fillRectangle(0, 1, defaultMoldsGui, false);
        fillRectangle(0, 3, storedMoldsGui, false);
    }

    /**
     * Refreshes all GUI sections to reflect current data state
     */
    private void refreshAllSections() {
        defaultMoldsGui.refresh();
        storedMoldsGui.refresh();
    }

    /**
     * Synchronizes the main smelter GUI if it exists and reopens it for the player
     */
    private void syncAndReopenMainSmelterGui(Player player) {
        if (smelterData.getGui() != null) {
            smelterData.getGui().syncFromStorage();
            // Close this GUI and reopen the main smelter GUI
            closeForAllViewers();
            smelterData.getGui().show(player);
        }
    }

    /**
     * Synchronizes the main smelter GUI if it exists (for operations that don't reopen)
     */
    private void syncMainSmelterGui() {
        if (smelterData.getGui() != null) {
            smelterData.getGui().syncFromStorage();
        }
    }

    @Override
    public @NotNull Component getTitle() {
        return Component.text("<shift:-48><glyph:menu_smelter_casting_mold_picker>").font(NEXO);
    }

    private class GuiDefaultMolds extends AbstractScrollGui<ItemInstance> {

        public GuiDefaultMolds() {
            super(9, 1, false, new Structure("<SSSSSSS>")
                    .addIngredient('S', Markers.CONTENT_LIST_SLOT_HORIZONTAL)
                    .addIngredient('<', new GuiSelectOne.ScrollLeftItem())
                    .addIngredient('>', new GuiSelectOne.ScrollRightItem())
            );

            // Because its column
            // InvUI doesnt calculate the width correctly (and, therefore, the amount)
            setLineAmount(7);
            setLineLength(1);
            // end

            refresh();
        }

        private void refresh() {
            // Get all unique default casting molds from registered recipes
            List<ItemInstance> defaultMolds = recipeRegistry.getAllRecipes().stream()
                    .map(CastingMoldRecipe::getBaseMold)
                    .distinct()
                    .map(itemFactory::create)
                    .collect(Collectors.toList());
            setContent(defaultMolds);
        }

        @Override
        public void bake() {
            ArrayList<SlotElement> elements = new ArrayList<>(content.size());
            for (ItemInstance item : content) {
                elements.add(new SlotElement.ItemSlotElement(new DefaultMold(item)));
            }

            this.elements = elements;
            update();
        }
    }

    private class GuiStoredMolds extends AbstractScrollGui<ItemInstance> {

        private GuiStoredMolds() {
            super(9, 1, false, new Structure("<SSSSSS>+")
                    .addIngredient('S', Markers.CONTENT_LIST_SLOT_HORIZONTAL)
                    .addIngredient('<', new GuiSelectOne.ScrollLeftItem())
                    .addIngredient('>', new GuiSelectOne.ScrollRightItem())
                    .addIngredient('+', new AddMoldButton())
            );
            refresh();
        }

        private void refresh() {
            setContent(smelterData.getProcessingEngine().getCastingMoldItems().getContent());
        }

        @Override
        public void bake() {
            ArrayList<SlotElement> elements = new ArrayList<>(content.size());
            for (ItemInstance item : content) {
                elements.add(new SlotElement.ItemSlotElement(new StoredMold(item)));
            }

            this.elements = elements;
            update();
        }
    }

    private class DefaultMold extends ControlItem<GuiDefaultMolds> {

        private final ItemInstance item;

        private DefaultMold(ItemInstance item) {
            this.item = item;
        }

        @Override
        public ItemProvider getItemProvider(GuiDefaultMolds gui) {
            if (item == null) {
                return ItemProvider.EMPTY;
            }

            ItemView.ItemViewBuilder builder = ItemView.of(item.getView().get()).toBuilder();

            // Highlight if this is the currently selected mold
            if (item.getBaseItem() instanceof CastingMold mold && mold.equals(smelterData.getProcessingEngine().getCastingMold())) {
                builder.glow(true);
                builder.lore(Component.empty());
                builder.lore(Component.text("Currently Selected", NamedTextColor.GREEN, TextDecoration.BOLD));
            }

            return builder.action(ClickActions.LEFT, Component.text("Select this mold"))
                    .build();
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
            if (item == null || clickType != ClickType.LEFT) {
                return;
            }

            if (item.getBaseItem() instanceof CastingMold mold) {
                smelterData.getProcessingEngine().setCastingMold(mold);

                SoundEffect.HIGH_PITCH_PLING.play(player);

                // Update and reopen the main smelter GUI
                syncAndReopenMainSmelterGui(player);
            }
        }
    }

    private class StoredMold extends ControlItem<GuiStoredMolds> {

        private final ItemInstance item;

        private StoredMold(ItemInstance item) {
            this.item = item;
        }

        @Override
        public ItemProvider getItemProvider(GuiStoredMolds gui) {
            if (item == null) {
                return ItemProvider.EMPTY;
            }

            ItemView.ItemViewBuilder builder = ItemView.of(item.getView().get()).toBuilder();

            // Highlight if this is the currently selected mold
            if (item.getBaseItem() instanceof CastingMold mold && mold.equals(smelterData.getProcessingEngine().getCastingMold())) {
                builder.glow(true);
                builder.lore(Component.empty());
                builder.lore(Component.text("Currently Selected", NamedTextColor.GREEN, TextDecoration.BOLD));
            }

            return builder.action(ClickActions.LEFT, Component.text("Select this mold"))
                    .action(ClickActions.RIGHT_SHIFT, Component.text("Remove", NamedTextColor.RED))
                    .build();
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
            if (item == null) {
                return;
            }

            if (clickType == ClickType.LEFT && item.getBaseItem() instanceof CastingMold mold) {
                smelterData.getProcessingEngine().setCastingMold(mold);

                SoundEffect.HIGH_PITCH_PLING.play(player);

                // Update and reopen the main smelter GUI
                syncAndReopenMainSmelterGui(player);
            } else if (clickType == ClickType.SHIFT_RIGHT) {
                // Remove from storage
                List<ItemInstance> currentStored = new ArrayList<>(smelterData.getProcessingEngine().getCastingMoldItems().getContent());
                currentStored.remove(item);
                smelterData.getProcessingEngine().getCastingMoldItems().setContent(currentStored);

                if (smelterData.getProcessingEngine().getCastingMold() == item.getBaseItem()) {
                    // If this was the currently selected mold, reset it
                    smelterData.getProcessingEngine().setCastingMold(null);
                }

                // Refund it to the player's inventory
                ItemStack itemStack = item.createItemStack();
                UtilItem.insert(player, itemStack);

                // Update all GUI sections to reflect the removal
                refreshAllSections();

                // Update the main smelter GUI if it exists
                syncMainSmelterGui();

                SoundEffect.LOW_PITCH_PLING.play(player);
            }
        }
    }

    private class AddMoldButton extends ControlItem<GuiStoredMolds> {

        @Override
        public ItemProvider getItemProvider(GuiStoredMolds gui) {
            return ItemView.builder()
                    .material(Material.PAPER)
                    .itemModel(Resources.ItemModel.INVISIBLE)
                    .displayName(Component.text("Add Casting Mold", NamedTextColor.GREEN))
                    .lore(Component.text("Drag a casting mold from your", NamedTextColor.GRAY))
                    .lore(Component.text("inventory and click this button", NamedTextColor.GRAY))
                    .lore(Component.text("to store it.", NamedTextColor.GRAY))
                    .build();
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
            ItemStack cursorItem = event.getCursor();

            // Check if player has an item on cursor
            if (cursorItem.getType().isAir()) {
                return;
            }

            // Try to convert the cursor item to ItemInstance and check if it's a casting mold
            final Optional<ItemInstance> instanceOpt = itemFactory.fromItemStack(cursorItem);
            if (instanceOpt.isEmpty()) {
                SoundEffect.WRONG_ACTION.play(player);
                return;
            }

            final ItemInstance itemInstance = instanceOpt.get();
            if (!(itemInstance.getBaseItem() instanceof CastingMold)) {
                // Not a casting mold
                SoundEffect.WRONG_ACTION.play(player);
                return;
            }

            // Disallow repeats
            if (smelterData.getProcessingEngine().getCastingMoldItems().getContent().stream()
                    .anyMatch(item -> item.getBaseItem().equals(itemInstance.getBaseItem()))) {
                SoundEffect.LOW_PITCH_PLING.play(player);
                return;
            }

            // It's a valid casting mold, add it to storage
            List<ItemInstance> currentStored = new ArrayList<>(smelterData.getProcessingEngine().getCastingMoldItems().getContent());

            // Create a copy of the item for storage (only take 1 if stack)
            ItemStack singleItem = cursorItem.clone();
            singleItem.setAmount(1);

            final ItemInstance storedStack = itemFactory.fromItemStack(singleItem).orElseThrow();
            currentStored.add(storedStack);
            smelterData.getProcessingEngine().getCastingMoldItems().setContent(currentStored);

            // Reduce cursor item by 1
            if (cursorItem.getAmount() > 1) {
                cursorItem.setAmount(cursorItem.getAmount() - 1);
                event.setCursor(cursorItem);
            } else {
                event.setCursor(null);
            }

            // Update all GUI sections to reflect the addition
            refreshAllSections();

            // Update the main smelter GUI if it exists
            syncMainSmelterGui();

            SoundEffect.HIGH_PITCH_PLING.play(player);
        }
    }
}
