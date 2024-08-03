package me.mykindos.betterpvp.champions.tips.tips.mage;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.tips.ChampionsTip;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

@Singleton
public class ArticArmourTip extends ChampionsTip {

    @Inject
    public ArticArmourTip(Champions champions) {
        super(champions, 1, 1, Component.empty()
                .append(Component.text("While active,")).appendSpace()
                .append(Component.text("Mage", Role.MAGE.getColor()))
                .append(UtilMessage.deserialize("'s Passive A Skill <white>Arctic Armour</white> gives " +
                        "nearby allies <white>Resistance</white> and nearby enemies <white>slowness</white>. " +
                        "Additionally, it will <white>freeze</white> nearby water, so try not to get trapped underneath! ")));
    }

    @Override
    public String getName() {
        return "arcticarmourtip";
    }

    @Override
    public boolean isValid(Player player, Role role) {
        return true;
    }
}
