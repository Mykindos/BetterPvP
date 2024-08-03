package me.mykindos.betterpvp.champions.tips.tips.weapons;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.tips.ChampionsTip;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.entity.Player;

@Singleton
public class BoosterWeaponTip extends ChampionsTip {

    @Inject
    public BoosterWeaponTip(Champions champions) {
        super(champions, 1, 1,
                UtilMessage.deserialize("<gold>Booster</gold> weapons (<gold>Gold</gold> weapons) increase the " +
                        "level of their corresponding skill by <yellow>1</yellow> level")
                );
    }

    @Override
    public String getName() {
        return "boosterweapontip";
    }

    @Override
    public boolean isValid(Player player, Role role) {
        return true;
    }
}
