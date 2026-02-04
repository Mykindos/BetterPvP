package me.mykindos.betterpvp.core.recipe.crafting.menu;

import com.google.inject.Inject;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.inventory.gui.structure.Structure;
import me.mykindos.betterpvp.core.inventory.window.Window;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.recipe.crafting.CraftingManager;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import static me.mykindos.betterpvp.core.utilities.Resources.Font.NEXO;

@CustomLog
public class GuiCraftingTable extends AbstractCraftingGui implements Windowed {

    @Inject
    private GuiCraftingTable(CraftingManager craftingManager, ItemFactory itemFactory, ClientManager clientManager) {
        super(craftingManager, itemFactory,clientManager, 9, 5);
        
        applyStructure(new Structure(
                "000000000",
                "0XXX00000",
                "0XXX00R00",
                "0XXX00000",
                "000000000")
                .addIngredient('X', craftingMatrix)
                .addIngredient('R', resultInventory));
    }

    @Override
    public Window show(@NotNull Player player) {
        final Window window = Windowed.super.show(player);
        // refund items in the crafting matrix
        window.addCloseHandler(() -> refund(player));
        return window;
    }

    @Override
    public @NotNull Component getTitle() {
        return Component.text("<shift:-48><glyph:menu_crafting_table>").font(NEXO);
    }
}
