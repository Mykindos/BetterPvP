package me.mykindos.betterpvp.core.menu.buttons;

import me.mykindos.betterpvp.core.gamer.Gamer;
import me.mykindos.betterpvp.core.menu.Button;
import me.mykindos.betterpvp.core.menu.Menu;
import me.mykindos.betterpvp.core.menu.MenuManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

public class BackButton extends Button {

    private final Menu previousMenu;

    public BackButton(int slot, ItemStack item, Menu previousMenu) {
        super(slot, item, Component.text("Back", NamedTextColor.RED));
        this.previousMenu = previousMenu;
    }

    @Override
    public void onClick(Player player, Gamer gamer, ClickType clickType) {
        MenuManager.openMenu(player, previousMenu);
    }
}
