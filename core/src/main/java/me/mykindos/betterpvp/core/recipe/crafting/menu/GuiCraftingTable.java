package me.mykindos.betterpvp.core.recipe.crafting.menu;

import com.google.inject.Inject;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.inventory.gui.structure.Structure;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.recipe.crafting.CraftingManager;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import static me.mykindos.betterpvp.core.utilities.Resources.Font.NEXO;

@CustomLog
public class GuiCraftingTable extends AbstractCraftingGui {

    @Inject
    private GuiCraftingTable(CraftingManager craftingManager, ItemFactory itemFactory) {
        super(craftingManager, itemFactory);
        
        applyStructure(new Structure(
                "000000000",
                "0XXX00000",
                "0XXX000R0",
                "0XXX00000",
                "000000000",
                "000000000")
                .addIngredient('X', craftingMatrix)
                .addIngredient('R', resultInventory));
    }

    @Override
    public @NotNull Component getTitle() {
        return Component.text("<shift:-48><glyph:menu_crafting_table>").font(NEXO);
    }
}
