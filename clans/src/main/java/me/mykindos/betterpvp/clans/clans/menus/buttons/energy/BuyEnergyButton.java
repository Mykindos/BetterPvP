package me.mykindos.betterpvp.clans.clans.menus.buttons.energy;

import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.events.ClanBuyEnergyEvent;
import me.mykindos.betterpvp.core.menu.Button;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public abstract class BuyEnergyButton extends Button {

    @NotNull
    private final Clan clan;

    public BuyEnergyButton(int slot, @NotNull Clan clan) {
        super(slot);
        this.clan = clan;
    }

    protected void buyEnergy(Player player, int amount, int cost) {
        UtilServer.callEvent(new ClanBuyEnergyEvent(player, clan, amount, cost));
    }
}
