package me.mykindos.betterpvp.clans.logging.button;

import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.logging.menu.ClansOfPlayerMenu;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.AbstractItem;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

public class PlayerButton extends AbstractItem {
    private final Client client;
    private final ClanManager clanManager;
    private final ClientManager clientManager;
    private final Windowed previous;

    public PlayerButton(Client client, ClanManager clanManager, ClientManager clientManager, Windowed previous) {
        this.client = client;
        this.clanManager = clanManager;
        this.clientManager = clientManager;
        this.previous = previous;
    }

    /**
     * Gets the {@link ItemProvider}.
     * This method gets called every time a {@link Window} is notified ({@link #notifyWindows()}).
     *
     * @return The {@link ItemProvider}
     */
    @Override
    public ItemProvider getItemProvider() {
        return ItemView.builder()
                .displayName(Component.text(client.getName(), NamedTextColor.YELLOW))
                .lore(Component.text(client.getUuid()))
                .material(Material.PLAYER_HEAD)
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
        new ClansOfPlayerMenu(client, clanManager, clientManager, previous).show(player);
    }
}
