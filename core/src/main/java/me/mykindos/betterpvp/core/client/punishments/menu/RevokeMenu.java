package me.mykindos.betterpvp.core.client.punishments.menu;

import me.mykindos.betterpvp.core.client.punishments.Punishment;
import me.mykindos.betterpvp.core.inventory.gui.AbstractGui;
import me.mykindos.betterpvp.core.inventory.gui.structure.Structure;
import me.mykindos.betterpvp.core.menu.Menu;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.menu.button.BackButton;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

public class RevokeMenu extends AbstractGui implements Windowed {
    /**
     * Creates a new {@link AbstractGui} with the specified width and height.
     *
     * @param width  The width of the Gui
     * @param height The height of the Gui
     */
    protected RevokeMenu(Punishment punishment, String revokeReason, PunishmentItem item, Windowed previous) {
        super(9, 3);




        Structure structure = new Structure("XXXXXXXXX",
                "XAXXPXXIX",
                "XXXXBXXXX")
                .addIngredient('X', Menu.BACKGROUND_ITEM)
                .addIngredient('B', new BackButton(previous))
                .addIngredient('P', item)
                .addIngredient('A', gameplayItem)
                .addIngredient('I', chatItem);

        applyStructure(structure);
    }

    /**
     * @return The title of this menu.
     */
    @Override
    public @NotNull Component getTitle() {
        return null;
    }
}
