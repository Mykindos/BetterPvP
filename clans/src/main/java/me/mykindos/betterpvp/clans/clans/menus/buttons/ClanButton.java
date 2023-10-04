package me.mykindos.betterpvp.clans.clans.menus.buttons;

import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanRelation;
import me.mykindos.betterpvp.core.components.clans.data.ClanEnemy;
import me.mykindos.betterpvp.core.menu.Button;
import me.mykindos.betterpvp.core.utilities.UtilItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class ClanButton extends Button {

    private final Clan clan;
    private final Player player;

    private enum ClanRelationship {
        SELF, ALLY, ENEMY, NEUTRAL
    }

    public ClanButton(int slot, Clan clan, Player player, ClanRelation relation) {
        super(slot, clan.getBanner());
        this.clan = clan;
        this.player = player;

        // Determine the name color based on the relationship
        NamedTextColor nameColor = relation.getSecondary();

        this.name = Component.text(clan.getName(), nameColor).decoration(TextDecoration.ITALIC,false);
        this.lore = new ArrayList<>();

        int netDominance = calculateNetDominance();
        TextComponent netDominanceValue = netDominance >= 0
                ? Component.text(netDominance + "%", NamedTextColor.GREEN)
                : Component.text(netDominance + "%", NamedTextColor.DARK_PURPLE);

        TextComponent netDominanceText = Component.text("Net Dominance: ", NamedTextColor.GRAY)
                .append(netDominanceValue)
                .decoration(TextDecoration.ITALIC, false);

        TextComponent clanRankText = Component.text("Clan Rank", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false);
        lore.add(netDominanceText);
        lore.add(clanRankText);

        this.itemStack = UtilItem.removeAttributes(UtilItem.setItemNameAndLore(itemStack, name, lore)).clone();
    }

    private int calculateNetDominance() {
        int netDominance = 0;
        List<ClanEnemy> enemies = clan.getEnemies();
        for (ClanEnemy enemy : enemies) {
            netDominance += enemy.getDominance();
        }
        return netDominance;
    }


}
