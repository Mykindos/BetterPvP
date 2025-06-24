package me.mykindos.betterpvp.core.block.impl.workbench;

import com.google.common.base.Preconditions;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.block.SmartBlockInstance;
import me.mykindos.betterpvp.core.inventory.gui.structure.Structure;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.AbstractItem;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.component.impl.blueprint.BlueprintComponent;
import me.mykindos.betterpvp.core.item.component.impl.blueprint.BlueprintItem;
import me.mykindos.betterpvp.core.recipe.crafting.CraftingManager;
import me.mykindos.betterpvp.core.recipe.crafting.menu.AbstractCraftingGui;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

import static me.mykindos.betterpvp.core.utilities.Resources.Font.NEXO;

@CustomLog
public class GuiWorkbench extends AbstractCraftingGui {

    private final GuiBlueprintViewer viewer;
    private final Workbench workbench;
    private final SmartBlockInstance blockInstance;

    public GuiWorkbench(CraftingManager craftingManager, ItemFactory itemFactory, SmartBlockInstance blockInstance) {
        super(craftingManager, itemFactory);
        Preconditions.checkState(blockInstance.getType() instanceof Workbench,
                "The block instance must be of type Workbench, but was: " + blockInstance.getType().getKey());

        this.blockInstance = blockInstance;
        this.workbench = (Workbench) blockInstance.getType();
        this.viewer = new GuiBlueprintViewer(blockInstance, workbench);

        // Setup GUI structure with crafting grid, result, quick crafts, and blueprint button
        applyStructure(new Structure(
                "000000000",
                "0XXX00000",
                "0XXX00R00",
                "0XXX00000",
                "000000B00",
                "000000000")
                .addIngredient('X', craftingMatrix)
                .addIngredient('R', resultInventory)
//                .addIngredient('Q', )
                .addIngredient('B', new BlueprintViewerButton()));
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
}