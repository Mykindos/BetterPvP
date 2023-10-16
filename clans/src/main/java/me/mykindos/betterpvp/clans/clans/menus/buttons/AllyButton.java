package me.mykindos.betterpvp.clans.clans.menus.buttons;

import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.menus.ClanMenu;
import me.mykindos.betterpvp.core.components.clans.data.ClanAlliance;
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

public class AllyButton extends Button {

    private final Clan playerClan;
    private final Clan allyClan;

    public AllyButton(int slot, Clan playerClan, ClanAlliance ally) {
        super(slot, getBannerItem(ally));
        this.playerClan = playerClan;
        this.allyClan = (Clan)ally.getClan();

        this.name = Component.text(allyClan.getName(), NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false);
        this.lore = new ArrayList<>();
        lore.add(UtilMessage.deserialize("%d/%d Online", allyClan.getOnlineMemberCount(), allyClan.getMembers().size()));

        this.itemStack = UtilItem.removeAttributes(UtilItem.setItemNameAndLore(itemStack, name, lore)).clone();
    }

    private static ItemStack getBannerItem(ClanAlliance ally) {
        Clan clan = (Clan) ally.getClan();
        return clan.getBanner();
    }

    @Override
    public void onClick(Player player, Gamer gamer, ClickType clickType) {
        if (clickType.isLeftClick()) {
            MenuManager.openMenu(player, new ClanMenu(player, playerClan, allyClan));
        }
    }


}