package me.mykindos.betterpvp.core.client.punishments.menu;

import me.mykindos.betterpvp.core.client.punishments.Punishment;
import me.mykindos.betterpvp.core.client.punishments.PunishmentHandler;
import me.mykindos.betterpvp.core.client.punishments.types.RevokeType;
import me.mykindos.betterpvp.core.inventory.gui.AbstractGui;
import me.mykindos.betterpvp.core.inventory.gui.structure.Structure;
import me.mykindos.betterpvp.core.inventory.item.Click;
import me.mykindos.betterpvp.core.inventory.item.impl.SimpleItem;
import me.mykindos.betterpvp.core.menu.Menu;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.menu.button.BackButton;
import me.mykindos.betterpvp.core.menu.impl.ConfirmationMenu;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class RevokeMenu extends AbstractGui implements Windowed {
    /**
     * Creates a new {@link AbstractGui} with the specified width and height.
     *
     * @param width  The width of the Gui
     * @param height The height of the Gui
     */
    protected RevokeMenu(Punishment punishment, String revokeReason, PunishmentItem item, PunishmentHandler punishmentHandler, Windowed previous) {
        super(9, 3);

        ItemView appealView = ItemView.builder()
                .displayName(Component.text(RevokeType.APPEAL.name()))
                .lore(Component.text("Valid punishment, being lifted early"))
                .material(Material.HOPPER)
                .customModelData(1)
                .build();

        Consumer<Click> appealClick = (click) -> {
            if (click.getClickType().isLeftClick()) {
                new ConfirmationMenu("Revoke this punishment as APPEAL? Reason: " + revokeReason, (success) -> {
                    if (Boolean.TRUE.equals(success)) {
                        punishmentHandler.revoke(punishment, click.getPlayer().getUniqueId(), RevokeType.APPEAL, revokeReason);
                    } else {
                        this.show(click.getPlayer());
                    }
                }).show(click.getPlayer());
            }
        };

        SimpleItem appealItem = new SimpleItem(appealView, appealClick);

        ItemView incorrectView = ItemView.builder()
                .displayName(Component.text(RevokeType.INCORRECT.name()))
                .lore(Component.text("Valid punishment, being lifted early"))
                .material(Material.REDSTONE_BLOCK)
                .customModelData(1)
                .build();

        Consumer<Click> incorrectClick = (click) -> {
            if (click.getClickType().isLeftClick()) {
                new ConfirmationMenu("Revoke this punishment as INCORRECT? Reason: " + revokeReason, (success) -> {
                    if (Boolean.TRUE.equals(success)) {
                        punishmentHandler.revoke(punishment, click.getPlayer().getUniqueId(), RevokeType.INCORRECT, revokeReason);
                    } else {
                        this.show(click.getPlayer());
                    }
                }).show(click.getPlayer());
            }
        };

        SimpleItem incorrectItem = new SimpleItem(incorrectView, incorrectClick);


        Structure structure = new Structure("XXXXXXXXX",
                "XAXXPXXIX",
                "XXXXBXXXX")
                .addIngredient('X', Menu.BACKGROUND_ITEM)
                .addIngredient('B', new BackButton(previous))
                .addIngredient('P', item)
                .addIngredient('A', appealItem)
                .addIngredient('I', incorrectItem);

        applyStructure(structure);
    }

    /**
     * @return The title of this menu.
     */
    @Override
    public @NotNull Component getTitle() {
        return Component.text("Revoke");
    }
}
