package me.mykindos.betterpvp.clans.clans.menus;

import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.menus.buttons.AllyButton;
import me.mykindos.betterpvp.core.components.clans.data.ClanAlliance;
import me.mykindos.betterpvp.core.menu.Menu;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;


import java.util.ArrayList;
import java.util.List;

public class AlliesMenu extends Menu {

    private final Clan playerClan;
    private final Clan clan;

    public AlliesMenu(Player player, Clan playerClan, Clan clan) {
        super(player, 54, Component.text("Allies"));
        this.playerClan = playerClan;
        this.clan = clan;
        fillPage();

    }

    public void fillPage() {
        List<ClanAlliance> allies = new ArrayList<>(clan.getAlliances());

        int slot = 0;
        for (ClanAlliance ally : allies) {
            // If slot exceeds 53 (0 indexed, so total 54 slots), break the loop
            if (slot > 53) {
                break;
            }

            addButton(new AllyButton(slot, playerClan, ally));
            slot++;
        }
    }

}