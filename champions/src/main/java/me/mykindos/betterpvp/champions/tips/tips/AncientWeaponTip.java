package me.mykindos.betterpvp.champions.tips.tips;

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
                UtilMessage.deserialize("<dark_purple>Ancient</dark_purple> weapons (Netherite Sword / Netherite Axe) combine " +
                        "the effects of <gold>booster</gold> and <aqua>power</aqua> weapons, boosting the sword / axe skill by " +
                        "<stat>1</stat> level and increasing melee damage by <stat>1</stat>")
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
