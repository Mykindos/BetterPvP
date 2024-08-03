package me.mykindos.betterpvp.champions.tips;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.tips.Tip;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

@Singleton
public abstract class ChampionsTip extends Tip {


    protected ChampionsTip(Champions champions, int defaultCategoryWeight, int defaultWeight, Component component) {
        super(champions, defaultCategoryWeight, defaultWeight, component);
    }

    protected ChampionsTip(Champions champions, int defaultCategoryWeight, int defaultWeight) {
        super(champions, defaultCategoryWeight, defaultWeight);
    }

    public boolean isValid(Player player, Role role) {
        return super.isValid(player);
    }

}
