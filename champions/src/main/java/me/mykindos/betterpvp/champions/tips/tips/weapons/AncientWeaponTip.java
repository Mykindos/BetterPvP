package me.mykindos.betterpvp.champions.tips.tips.weapons;

import com.google.inject.Inject;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.tips.ChampionsTip;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.entity.Player;

public class AncientWeaponTip extends ChampionsTip {
    @Inject
    public AncientWeaponTip(Champions champions) {
        super(champions, 1, 1,
                UtilMessage.deserialize("<dark_purple>Ancient</dark_purple> weapons (<dark_purple>Netherite</dark_purple> weapons) combine " +
                        "the effects of <gold>booster</gold> and <aqua>power</aqua> weapons, boosting the corresponding skill by " +
                        "<yellow>1</yellow> level and increasing melee damage by <yellow>1</yellow>")
        );
    }

    @Override
    public String getName() {
        return "ancientweapontip";
    }

    @Override
    public boolean isValid(Player player, Role role) {
        return true;
    }
}
