package me.mykindos.betterpvp.champions.champions.builds.menus.buttons;

import me.mykindos.betterpvp.champions.champions.builds.menus.ClassSelectionMenu;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.controlitem.ControlItem;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.ComponentWrapper;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.utilities.model.item.ClickActions;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.jetbrains.annotations.NotNull;

/**
 * A class's helmet in the class selector. Clicking it makes the class the one the rest of the menu describes.
 */
public class ClassSelectionButton extends ControlItem<ClassSelectionMenu> {

    private static final int DESCRIPTION_WIDTH = 35;

    private final Role role;

    public ClassSelectionButton(Role role) {
        this.role = role;
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        if (!ClickActions.LEFT.accepts(clickType)) {
            SoundEffect.WRONG_ACTION.play(player);
            return;
        }

        if (getGui().getSelected() == role) {
            return;
        }

        getGui().setSelected(role);
        getGui().updateControlItems();
        SoundEffect.HIGH_PITCH_PLING.play(player);
    }

    @Override
    public ItemProvider getItemProvider(ClassSelectionMenu gui) {
        return ItemView.builder()
                .material(role.getHelmet())
                .displayName(role.getDisplayName().color(role.getColor()).decorate(TextDecoration.BOLD))
                .lore(Component.empty())
                .lore(ComponentWrapper.markForWrap(role.getSummaryComponent(), DESCRIPTION_WIDTH))
                .action(ClickActions.LEFT, Translations.component("champions.menu.class.select"))
                .flag(ItemFlag.HIDE_ATTRIBUTES)
                .glow(gui.getSelected() == role)
                .hideAdditionalTooltip(true)
                .build();
    }
}
