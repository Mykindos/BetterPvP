package me.mykindos.betterpvp.clans.clans.tips.tips;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.tips.ClanTip;
import me.mykindos.betterpvp.core.tips.BPvPTip;
import me.mykindos.betterpvp.core.tips.Tip;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

@Singleton
@BPvPTip
public class ClanEnergyTip extends ClanTip {

    ClanEnergyTip() {
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
