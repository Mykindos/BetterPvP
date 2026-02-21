package me.mykindos.betterpvp.core.client.stats.display.championsgame.domination.team;

import me.mykindos.betterpvp.core.client.stats.display.IAbstractStatMenu;
import me.mykindos.betterpvp.core.client.stats.display.championsgame.DominationStatButton;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

public class DominationTeamMapInfoStatButton extends DominationStatButton {
    private final String mapName;
    private final String teamName;

    public DominationTeamMapInfoStatButton(String mapName, String teamName) {
        this.mapName = mapName;
        this.teamName = teamName;
    }

    /**
     * Gets the {@link ItemProvider}.
     * This method gets called every time a {@link Window} is notified ({@link #notifyWindows()}).
     *
     * @param gui
     * @return The {@link ItemProvider}
     */
    @Override
    public ItemProvider getItemProvider(IAbstractStatMenu gui) {

        return ItemView.builder()
                .material(teamName.equals("Blue") ? Material.BLUE_CARPET : Material.RED_CARPET)
                .displayName(Component.text(mapName + " Stats"))
                .lore(getDominationStatsDescription(teamName, mapName))
                .frameLore(true)
                .build();
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
        //does nothing
    }
}
