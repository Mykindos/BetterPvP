package me.mykindos.betterpvp.clans.tips.tips;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.tips.ClanTip;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

@Singleton
public class ClanEnergyTip extends ClanTip {

    public ClanEnergyTip() {
        super(2, 1, Component.text("You can purchase energy in the shops", NamedTextColor.GRAY));
    }

    @Override
    public String getName() {
        return "clanenergy";
    }

    public  boolean isValid(Player player, Clan clan) {
        return clan != null;
    }

}
