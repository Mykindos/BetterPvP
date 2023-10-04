package me.mykindos.betterpvp.clans.clans.menus.buttons;

import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.menus.AlliesMenu;
import me.mykindos.betterpvp.core.gamer.Gamer;
import me.mykindos.betterpvp.core.menu.Button;
import me.mykindos.betterpvp.core.menu.MenuManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

public class AlliesButton extends Button {

    private final Clan playerClan;
    private final Clan clan;

    public AlliesButton(int slot, Clan playerClan, Clan clan) {
        super(slot, new ItemStack(Material.PAPER), Component.text("Allies", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC,false),
                Component.text(clan.getOnlineAllyCount() + "/" + (clan.getAlliances().size()) + " Online",NamedTextColor.GRAY).decoration(TextDecoration.ITALIC,false));
        this.playerClan = playerClan;
        this.clan = clan;

    }

    @Override
    public void onClick(Player player, Gamer gamer, ClickType clickType) {
        if (clickType.isLeftClick()) {
            MenuManager.openMenu(player, new AlliesMenu(player, playerClan, clan));
        }
    }

}
