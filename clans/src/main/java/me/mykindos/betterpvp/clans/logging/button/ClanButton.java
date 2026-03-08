package me.mykindos.betterpvp.clans.logging.button;

import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.logging.menu.PlayersOfClanMenu;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.inventory.item.Item;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.AbstractItem;
import me.mykindos.betterpvp.core.inventory.window.Window;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class ClanButton extends AbstractItem {
    private final String name;
    private final long id;
    private final ClanManager clanManager;
    private final ClientManager clientManager;
    private final Windowed previous;

    public ClanButton(String name, long id, ClanManager clanManager, ClientManager clientManager, Windowed previous) {
        this.name = name;
        this.id = id;
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
        ItemStack banner = ItemStack.of(Material.GRAY_BANNER);
        Optional<Clan> clanOptional = clanManager.getClanById(id);
        if (clanOptional.isPresent()) {
            banner = clanOptional.get().getBanner().get();
        }
        return ItemView.builder()
                .displayName(Component.text(name, NamedTextColor.AQUA))
                .lore(Component.text(id))
                .with(banner)
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
        new PlayersOfClanMenu(name, id, clanManager, clientManager, previous).show(player);
    }
}
