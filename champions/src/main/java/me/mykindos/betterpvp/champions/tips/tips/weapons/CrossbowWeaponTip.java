package me.mykindos.betterpvp.champions.tips.tips.weapons;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.tips.ChampionsTip;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

@Singleton
public class CrossbowWeaponTip extends ChampionsTip {

    @Inject
    @Config(path = "combat.crossbow.cooldownEnabled")
    private boolean crossbowCooldownEnabled;

    @Inject
    @Config(path = "combat.crossbow.cooldownDuration")
    private double crossbowCooldownDuration;

    @Inject
    public CrossbowWeaponTip(Champions champions) {
        super(champions, 1, 1);
    }

    @Override
    public Component generateComponent() {
        Component component = UtilMessage.deserialize("Crossbows <gold>boost</gold> bow skills by <yellow>1</yellow> level. Certain skills require " +
                "the crossbow to be loaded to use.");
        if (crossbowCooldownEnabled) {
            component = component.appendSpace().append(UtilMessage.deserialize("Crossbows can be fired every <yellow>%s</yellow> seconds", crossbowCooldownDuration));
        }

        return component;
    }

    @Override
    public String getName() {
        return "crossbowweapontip";
    }

    @Override
    public boolean isValid(Player player, Role role) {
        return true;
    }
}
