package me.mykindos.betterpvp.champions.tips.tips;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.tips.ChampionsTip;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.entity.Player;

@Singleton
public class ReinforcedArmourTip extends ChampionsTip {

    @Inject
    public ReinforcedArmourTip(Champions champions) {
        super(champions, 1, 1,
                UtilMessage.deserialize(
                        "Crafted armor sets grant significantly more health than the default armor sets."
                )
        );
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
