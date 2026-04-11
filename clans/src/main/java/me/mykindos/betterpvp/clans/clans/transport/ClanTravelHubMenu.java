package me.mykindos.betterpvp.clans.clans.transport;

import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.core.client.Client;
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
    private final Client client;

    public ClanTravelHubMenu(Player player, Client client, ClanManager clanManager) {
        super(9, 5);
        this.player = player;
        this.clanManager = clanManager;
        this.client = client;

        loadMenu();
    }

    @Override
    public @NotNull Component getTitle() {
        return Component.text("Travel Hub");
    }

    private Integer getShopIndex(Clan clan) {
        var core = clan.getCore().getPosition();
        if (core == null) {
            return null;
        }

        double x = core.getX();
        double z = core.getZ();

        if (x < 0 && z < 0) return 2;
        if (x > 0 && z < 0) return 6;
        if (x < 0 && z > 0) return 38;
        if (x > 0 && z > 0) return 42;

        return null;
    }

    private void loadMenu() {

        // Shops
        clanManager.getClanByName("Blue Shops").ifPresent(clan -> {
            addShop(clan, Material.BLUE_WOOL, NamedTextColor.BLUE);
        });

        clanManager.getClanByName("Yellow Shops").ifPresent(clan -> {
            addShop(clan, Material.YELLOW_WOOL, NamedTextColor.YELLOW);
        });

        clanManager.getClanByName("Green Shops").ifPresent(clan -> {
            addShop(clan, Material.GREEN_WOOL, NamedTextColor.GREEN);
        });

        clanManager.getClanByName("Red Shops").ifPresent(clan -> {
            addShop(clan, Material.RED_WOOL, NamedTextColor.RED);
        });

        // Clan Home
        clanManager.getClanByPlayer(player).ifPresent(clan -> {
            setItem(22, new CoreTransportButton(clan));
        });
    }

    private void addShop(Clan clan, Material material, NamedTextColor color) {
        Integer slot = getShopIndex(clan);
        if (slot == null) {
            return;
        }

        setItem(slot, new ShopTransportButton(clan, client, material, color));
    }
}
