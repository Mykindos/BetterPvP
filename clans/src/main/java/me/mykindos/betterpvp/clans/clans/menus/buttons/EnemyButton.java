package me.mykindos.betterpvp.clans.clans.menus.buttons;

import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.menus.ClanMenu;
import me.mykindos.betterpvp.core.components.clans.data.ClanEnemy;
import me.mykindos.betterpvp.core.gamer.Gamer;
import me.mykindos.betterpvp.core.menu.Button;
import me.mykindos.betterpvp.core.menu.Menu;
import me.mykindos.betterpvp.core.menu.MenuManager;
import me.mykindos.betterpvp.core.utilities.UtilItem;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;

public class EnemyButton extends Button {

    private final Clan playerClan;
    private final Clan enemyClan;

    public EnemyButton(int slot, Clan playerClan, ClanEnemy enemy) {
        super(slot, getBannerItem(enemy));
        this.playerClan = playerClan;
        this.enemyClan = (Clan) enemy.getClan();

        this.name = Component.text(enemyClan.getName(), NamedTextColor.RED).decoration(TextDecoration.ITALIC, false);
        this.lore = new ArrayList<>();
        lore.add(UtilMessage.deserialize("%d/%d Online", enemyClan.getOnlineMemberCount(), enemyClan.getMembers().size()));

        this.itemStack = UtilItem.removeAttributes(UtilItem.setItemNameAndLore(itemStack, name, lore)).clone();
    }

    private static ItemStack getBannerItem(ClanEnemy enemy) {
        Clan clan = (Clan) enemy.getClan();
        return clan.getBanner();
    }

    @Override
    public void onClick(Player player, Gamer gamer, ClickType clickType) {
        if (clickType.isLeftClick()) {
            MenuManager.openMenu(player, new ClanMenu(player, playerClan, enemyClan));
        }
    }

}
