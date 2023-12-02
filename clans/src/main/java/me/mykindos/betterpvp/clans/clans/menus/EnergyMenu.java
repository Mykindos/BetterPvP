package me.mykindos.betterpvp.clans.clans.menus;

import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.menus.buttons.BuyEnergyButton;
import me.mykindos.betterpvp.core.menu.Menu;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.menu.button.BackButton;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.invui.gui.AbstractGui;

public class EnergyMenu extends AbstractGui implements Windowed {

    public EnergyMenu(Clan clan, Windowed previous) {
        super(9, 3);

        final int hour = (int) clan.getEnergyRatio();
        setItem(11, new BuyEnergyButton(clan, "1 Hour", hour, (int) (hour * 5.0)));
        setItem(13, new BuyEnergyButton(clan, "1 Day", hour * 24, (int) (hour * 24 * 5.0)));
        setItem(15, new BuyEnergyButton(clan, "1,000", 1_000, 5_000));
        setItem(26, new BackButton(previous));

        setBackground(Menu.BACKGROUND_ITEM);
    }

    @NotNull
    @Override
    public Component getTitle() {
        return Component.text("Energy Shop");
    }
}
