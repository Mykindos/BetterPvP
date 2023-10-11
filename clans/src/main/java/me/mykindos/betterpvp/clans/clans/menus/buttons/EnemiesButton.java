package me.mykindos.betterpvp.clans.clans.menus.buttons;

import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.menus.EnemiesMenu;
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

public class EnemiesButton extends Button {

    private final Clan playerClan;
    private final Clan clan;

    public EnemiesButton(int slot, Clan playerClan, Clan clan) {
        super(slot, new ItemStack(Material.PAPER), Component.text("Enemies", NamedTextColor.DARK_RED).decoration(TextDecoration.ITALIC, false),
                Component.text(clan.getOnlineEnemyCount() + "/" + (clan.getEnemies().size()) + " Online", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        this.playerClan = playerClan;
        this.clan = clan;
    }

    @Override
    public void onClick(Player player, Gamer gamer, ClickType clickType) {
        if (clickType.isLeftClick()) {
            openEnemiesMenu(player);
        }
    }

    private void openEnemiesMenu(Player player) {
        EnemiesMenu enemiesMenu = new EnemiesMenu(player, playerClan, clan);
        MenuManager.openMenu(player, enemiesMenu);
    }
}
