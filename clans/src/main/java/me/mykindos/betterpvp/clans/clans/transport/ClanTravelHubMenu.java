package me.mykindos.betterpvp.clans.clans.transport;

import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.core.inventory.gui.AbstractGui;
import me.mykindos.betterpvp.core.menu.Windowed;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ClanTravelHubMenu extends AbstractGui implements Windowed {

    private final Player player;
    private final ClanManager clanManager;

    public ClanTravelHubMenu(Player player, ClanManager clanManager) {
        super(9, 5);
        this.player = player;
        this.clanManager = clanManager;

        loadMenu();
    }

    @Override
    public @NotNull Component getTitle() {
        return Component.text("Travel Hub");
    }

    private void loadMenu() {

        // Spawns
        clanManager.getClanByName("North Spawn").ifPresent(clan -> {
            setItem(4, new SpawnTransportButton(clan, Material.END_CRYSTAL, NamedTextColor.WHITE));
        });

        clanManager.getClanByName("South Spawn").ifPresent(clan -> {
            setItem(40, new SpawnTransportButton(clan, Material.END_CRYSTAL, NamedTextColor.WHITE));
        });

        // Shops
        clanManager.getClanByName("Red Shops").ifPresent(clan -> {
            setItem(2, new ShopTransportButton(clan, Material.RED_WOOL, NamedTextColor.RED));
        });

        clanManager.getClanByName("Yellow Shops").ifPresent(clan -> {
            setItem(38, new ShopTransportButton(clan, Material.YELLOW_WOOL, NamedTextColor.YELLOW));
        });

        clanManager.getClanByName("Green Shops").ifPresent(clan -> {
            setItem(42, new ShopTransportButton(clan, Material.GREEN_WOOL, NamedTextColor.GREEN));
        });

        clanManager.getClanByName("Blue Shops").ifPresent(clan -> {
            setItem(6, new ShopTransportButton(clan, Material.BLUE_WOOL, NamedTextColor.BLUE));
        });

        // Clan Home
        clanManager.getClanByPlayer(player).ifPresent(clan -> {
            setItem(22, new HomeTransportButton(clan));
        });
    }
}
