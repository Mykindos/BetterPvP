package me.mykindos.betterpvp.clans.clans.menus;

import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.menus.buttons.EnemyButton;
import me.mykindos.betterpvp.core.components.clans.data.ClanEnemy;
import me.mykindos.betterpvp.core.menu.Menu;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;


import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class EnemiesMenu extends Menu {

    private final Clan playerClan;
    private final Clan clan;

    public EnemiesMenu(Player player, Clan playerClan, Clan clan) {
        super(player, 54, Component.text("Enemies"));
        this.playerClan = playerClan;
        this.clan = clan;
        fillPage();

    }

    public void fillPage() {
        List<ClanEnemy> enemies = new ArrayList<>(clan.getEnemies());
        enemies.sort(Comparator.comparingInt(e -> Math.abs(e.getDominance())));

        int slot = 0;
        for (ClanEnemy enemy : enemies) {
            // If slot exceeds 53 (0 indexed, so total 54 slots), break the loop
            if (slot > 53) {
                break;
            }

            addButton(new EnemyButton(slot, playerClan, enemy));
            slot++;
        }
    }

}
