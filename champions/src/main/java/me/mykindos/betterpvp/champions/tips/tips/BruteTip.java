package me.mykindos.betterpvp.champions.tips.tips;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.tips.ChampionsTip;
import me.mykindos.betterpvp.core.components.champions.Role;
import org.bukkit.entity.Player;

@Singleton
public class BruteTip extends ChampionsTip {

    @Inject
    public BruteTip(Champions champions) {
        super(champions, 1, 1, Role.BRUTE.getDescriptionComponent());
    }

    @Override
    public String getName() {
        return "brutetip";
    }

    @Override
    public boolean isValid(Player player, Role role) {
        return true;
    }
}
