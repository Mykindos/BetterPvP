package me.mykindos.betterpvp.core.block.impl.forge;

import com.google.common.base.Preconditions;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.block.SmartBlockInstance;
import me.mykindos.betterpvp.core.block.impl.workbench.GuiBlueprintViewer;
import me.mykindos.betterpvp.core.block.impl.workbench.Workbench;
import me.mykindos.betterpvp.core.inventory.gui.AbstractGui;
import me.mykindos.betterpvp.core.inventory.gui.structure.Structure;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.AbstractItem;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.component.impl.blueprint.BlueprintComponent;
import me.mykindos.betterpvp.core.item.component.impl.blueprint.BlueprintItem;
import me.mykindos.betterpvp.core.menu.Windowed;
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

import static me.mykindos.betterpvp.core.utilities.Resources.Font.NEXO;

@CustomLog
public class GuiForge extends AbstractGui implements Windowed {

    private final Forge forge;
    private final SmartBlockInstance blockInstance;

    public GuiForge(ItemFactory itemFactory, SmartBlockInstance blockInstance) {
        super(9, 6);
        Preconditions.checkState(blockInstance.getSmartBlock() instanceof Forge,
                "The block instance must be of type Forge, but was: " + blockInstance.getSmartBlock().getKey());

        this.blockInstance = blockInstance;
        this.forge = (Forge) blockInstance.getSmartBlock();

        applyStructure(new Structure(
                "000000000",
                "000000000",
                "000000000",
                "000000000",
                "000000B00",
                "000000000"));
    }

    @Override
    public @NotNull Component getTitle() {
        return Component.text("<shift:-48><glyph:menu_forge>").font(NEXO);
    }
}