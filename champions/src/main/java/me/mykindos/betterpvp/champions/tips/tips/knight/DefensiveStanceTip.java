package me.mykindos.betterpvp.champions.tips.tips.knight;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.tips.ChampionsTip;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

@Singleton
public class DefensiveStanceTip extends ChampionsTip {

    @Inject
    public DefensiveStanceTip(Champions champions) {
        super(champions, 1, 1, Component.empty()
                .append(Component.text("Knight", Role.KNIGHT.getColor()))
                .append(UtilMessage.deserialize("'s Sword Skill <white>Defensive Stance</white> " +
                        "while active prevents melee damage from players directly in front of them. " +
                        "Try hitting them from behind or using skills to damage them!")));
    }

    @Override
    public String getName() {
        return "defensivestancetip";
    }

    @Override
    public boolean isValid(Player player, Role role) {
        return true;
    }
}
