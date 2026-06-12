package me.mykindos.betterpvp.champions.tips.tips;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.tips.ChampionsTip;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.locale.Translations;
import org.bukkit.entity.Player;

@Singleton
public class ReinforcedArmourTip extends ChampionsTip {

    @Inject
    public ReinforcedArmourTip(Champions champions) {
        super(champions, 1, 1, Translations.component("champions.tip.reinforcedarmour"));
    }

    @Override
    public String getName() {
        return "reinforcedarmour";
    }

    @Override
    public boolean isValid(Player player, Role role) {
        return true;
    }
}
