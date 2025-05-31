package me.mykindos.betterpvp.core.client.achievements.display.button;

import me.mykindos.betterpvp.core.client.achievements.display.AchievementMenu;
import me.mykindos.betterpvp.core.client.achievements.display.Showing;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.controlitem.ControlItem;
import me.mykindos.betterpvp.core.utilities.model.item.ClickActions;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

public class PropertyContainerButton extends ControlItem<AchievementMenu> {
    private final Showing type;

    public PropertyContainerButton(Showing type) {
        this.type = type;
    }

    @Override
    public ItemProvider getItemProvider(AchievementMenu gui) {
        boolean selected = gui.getCurrent() == type;

        ItemView.ItemViewBuilder itemViewBuilder = ItemView.builder()
                .material(type.getMaterial())
                .displayName(Component.text(selected ? ">" + type.getName() + "<" : type.getName(), selected ? NamedTextColor.GREEN : NamedTextColor.GOLD));
        if (!selected) {
            itemViewBuilder.action(ClickActions.ALL, Component.text("Select", NamedTextColor.YELLOW));
        }

        return itemViewBuilder.build();
    }

    /**
     * A method called if the {@link ItemStack} associated to this {@link Item}
     * has been clicked by a player.
     *
     * @param clickType The {@link ClickType} the {@link Player} performed.
     * @param player    The {@link Player} who clicked on the {@link ItemStack}.
     * @param event     The {@link InventoryClickEvent} associated with this click.
     */
    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        getGui().setCurrent(type);
    }
}
