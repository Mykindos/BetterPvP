package me.mykindos.betterpvp.clans.clans.menus;

import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.menus.buttons.energy.OneDayEnergyButton;
import me.mykindos.betterpvp.clans.clans.menus.buttons.energy.OneHourEnergyButton;
import me.mykindos.betterpvp.clans.clans.menus.buttons.energy.OneThousandEnergyButton;
import me.mykindos.betterpvp.core.menu.Menu;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;

public class EnergyMenu extends Menu {

    private final Clan clan;

    public EnergyMenu(Player player, Clan clan) {
        super(player, 9, Component.text("Energy Shop").decoration(TextDecoration.ITALIC, false));
        this.clan = clan;

        addButton(new OneHourEnergyButton(1, clan));
        addButton(new OneDayEnergyButton(4, clan));
        addButton(new OneThousandEnergyButton(7, clan));

    }
}
