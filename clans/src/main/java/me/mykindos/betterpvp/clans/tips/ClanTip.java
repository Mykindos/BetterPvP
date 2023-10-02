package me.mykindos.betterpvp.clans.tips;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.core.tips.Tip;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

@Singleton
public abstract class ClanTip extends Tip {

    public ClanTip(int categoryWeight, int weight, Component component) {
        super(categoryWeight, weight, component);
    }

    public ClanTip(int categoryWeight, int weight) {
        super(categoryWeight, weight);
    }

    public boolean isValid(Player player, Clan clan) {
        return super.isValid(player);
    }

}
