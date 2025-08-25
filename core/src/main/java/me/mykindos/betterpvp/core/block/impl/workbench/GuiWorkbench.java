package me.mykindos.betterpvp.core.block.impl.workbench;

import com.google.common.base.Preconditions;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.block.SmartBlockInstance;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.inventory.gui.structure.Structure;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.AbstractItem;
import me.mykindos.betterpvp.core.inventory.item.impl.controlitem.ControlItem;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.component.impl.blueprint.BlueprintComponent;
import me.mykindos.betterpvp.core.item.component.impl.blueprint.BlueprintItem;
import me.mykindos.betterpvp.core.recipe.RecipeIngredient;
import me.mykindos.betterpvp.core.recipe.crafting.CraftingManager;
import me.mykindos.betterpvp.core.recipe.crafting.CraftingRecipe;
import me.mykindos.betterpvp.core.recipe.crafting.menu.AbstractCraftingGui;
import me.mykindos.betterpvp.core.recipe.crafting.resolver.HasIngredientsParameter;
import me.mykindos.betterpvp.core.utilities.UtilItem;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.utilities.model.item.ClickActions;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static me.mykindos.betterpvp.core.utilities.Resources.Font.NEXO;

@CustomLog
public class GuiWorkbench extends AbstractCraftingGui {

    private final GuiBlueprintViewer viewer;
    private final Workbench workbench;
    private final SmartBlockInstance blockInstance;
    private LinkedList<CraftingRecipe> quickCrafts = new LinkedList<>();
    private final HasIngredientsParameter lookupParameter;

    public GuiWorkbench(Player player, CraftingManager craftingManager, ItemFactory itemFactory, SmartBlockInstance blockInstance) {
        super(craftingManager, itemFactory);
        Preconditions.checkState(blockInstance.getType() instanceof Workbench,
                "The block instance must be of type Workbench, but was: " + blockInstance.getType().getKey());

        this.blockInstance = blockInstance;
        this.workbench = (Workbench) blockInstance.getType();
        this.viewer = new GuiBlueprintViewer(blockInstance, workbench);
        this.lookupParameter = new HasIngredientsParameter(player, this.itemFactory);

        this.updateQuickCrafts();

        // Setup GUI structure with crafting grid, result, quick crafts, and blueprint button
        applyStructure(new Structure(
                "000000000",
                "0XXX0000H",
                "0XXX00R0I",
                "0XXX0000J",
                "000000B00",
                "000000000")
                .addIngredient('X', craftingMatrix)
                .addIngredient('R', resultInventory)
                .addIngredient('H', new QuickCraftingButton(0))
                .addIngredient('I', new QuickCraftingButton(1))
                .addIngredient('J', new QuickCraftingButton(2))
                .addIngredient('B', new BlueprintViewerButton()));
    }

    public void updateQuickCrafts() {
        this.quickCrafts = this.craftingManager.getRegistry()
                .getResolver()
                .lookup(lookupParameter);
    }

    @Override
    protected List<BlueprintComponent> getBlueprints() {
        return ((WorkbenchData) Objects.requireNonNull(blockInstance.getData())).getContent().stream()
                .map(instance -> instance.getComponent(BlueprintComponent.class).orElseThrow())
                .toList();
    }

    @Override
    public @NotNull Component getTitle() {
        return Component.text("<shift:-48><glyph:menu_workbench>").font(NEXO);
    }

    private class BlueprintViewerButton extends AbstractItem {

        @Override
        public ItemProvider getItemProvider() {
            return ItemView.of(BlueprintItem.model).toBuilder()
                    .displayName(Component.text("View Blueprints", TextColor.color(66, 135, 245)))
                    .flag(ItemFlag.HIDE_ATTRIBUTES)
                    .build();
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
            viewer.show(player);
        }
    }

    private class QuickCraftingButton extends ControlItem<GuiWorkbench> {

        private final int slot;

        private QuickCraftingButton(int slot) {
            this.slot = slot;
        }

        @Override
        public ItemProvider getItemProvider(GuiWorkbench gui) {
            if (slot < quickCrafts.size()) {
                CraftingRecipe recipe = quickCrafts.get(slot);
                return ItemView.of(GuiWorkbench.this.itemFactory.create(recipe.getPrimaryResult()).getView().get())
                        .toBuilder()
                        .action(ClickActions.ALL, Component.text("Select"))
                        .build();
            } else {
                return ItemView.builder()
                        .material(Material.BARRIER)
                        .displayName(Component.text("No Quick Craft", TextColor.color(255, 0, 0)))
                        .build();
            }
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
            // No recipe
            if (slot >= quickCrafts.size()) {
                return;
            }

            // Invalid click
            if (clickType != ClickType.LEFT && clickType != ClickType.RIGHT) {
                SoundEffect.WRONG_ACTION.play(player);
                return;
            }

            // Refund the player the items in the matrix
            final @Nullable ItemStack[] items = GuiWorkbench.this.craftingMatrix.getItems();
            for (int i = 0; i < items.length; i++) {
                ItemStack item = items[i];
                if (item != null) {
                    UtilItem.insert(player, item);
                }
                GuiWorkbench.this.craftingMatrix.setItemSilently(i, null);
            }

            // Place the ingredients for the recipe in the matrix
            final CraftingRecipe recipe = quickCrafts.get(slot);
            final @Nullable ItemStack[] contents = player.getInventory().getStorageContents();
            if (lookupParameter.removeMatching(recipe, contents)) {
                player.getInventory().setStorageContents(contents);
            }

            for (Map.Entry<Integer, RecipeIngredient> entry : recipe.getIngredients().entrySet()) {
                final Integer slot = entry.getKey();
                final RecipeIngredient ingredient = entry.getValue();
                final ItemInstance itemInstance = itemFactory.create(ingredient.getBaseItem());
                itemInstance.getItemStack().setAmount(ingredient.getAmount());
                GuiWorkbench.this.craftingMatrix.setItemSilently(slot, itemInstance.getItemStack());
            }

            SoundEffect.HIGH_PITCH_PLING.play(player);
            updateQuickCrafts();
        }
    }
}