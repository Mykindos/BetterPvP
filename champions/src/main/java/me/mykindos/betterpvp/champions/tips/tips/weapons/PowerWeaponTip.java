package me.mykindos.betterpvp.champions.tips.tips.weapons;

import com.google.inject.Inject;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.tips.ChampionsTip;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.entity.Player;

public class PowerWeaponTip extends ChampionsTip {
    @Inject
    public PowerWeaponTip(Champions champions) {
        super(champions, 1, 1,
                UtilMessage.deserialize("<aqua>Power</aqua> weapons (<aqua>Diamond</aqua> weapons) increase " +
                        "melee damage by <yellow>1</yellow>")
        );
    }

    @Override
    public String getName() {
        return "powerweapontip";
    }

    @Override
    public boolean isValid(Player player, Role role) {
        return true;
    }
}
