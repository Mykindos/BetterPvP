package me.mykindos.betterpvp.clans.clans.tips.tips;

import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.tips.Tip;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

public class ClanEnergyTip extends Tip {

    ClanEnergyTip() {
        super(2, 1, Component.text("You can purchase energy in the shops", NamedTextColor.GRAY));
    }

    @Override
    public  boolean isValid(Player player, Clan clan) {
        return clan != null;
    }

}
