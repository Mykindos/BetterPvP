package me.mykindos.betterpvp.clans.clans.tips;

import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.core.tips.Tip;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

public abstract class ClanTip extends Tip {
    protected ClanTip(int categoryWeight, int weight) {
        super(categoryWeight, weight);
    }

    protected ClanTip(int categoryWeight, int weight, Component component) {
        super(categoryWeight, weight, component);
    }


    public boolean isValid(Player player, Clan clan) {
        return super.isValid(player);
    }

}
