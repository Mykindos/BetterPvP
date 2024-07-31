package me.mykindos.betterpvp.clans.tips;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.core.tips.Tip;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

@Singleton
public abstract class ClanTip extends Tip {


    protected ClanTip(Clans clans, int defaultCategoryWeight, int defaultWeight, Component component) {
        super(clans, defaultCategoryWeight, defaultWeight, component);
    }

    protected ClanTip(Clans clans, int defaultCategoryWeight, int defaultWeight) {
        super(clans, defaultCategoryWeight, defaultWeight);
    }

    public boolean isValid(Player player, Clan clan) {
        return super.isValid(player);
    }

}
