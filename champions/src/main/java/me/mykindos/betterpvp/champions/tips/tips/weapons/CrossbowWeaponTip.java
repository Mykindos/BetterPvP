package me.mykindos.betterpvp.champions.tips.tips.weapons;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.tips.ChampionsTip;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.locale.Translations;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
        setComponent(generateComponent());
    }

    @Override
    public Component generateComponent() {
        Component component = Translations.component("champions.tip.crossbowweapontip",
                Component.text("boost", NamedTextColor.GOLD),
                Component.text("1", NamedTextColor.YELLOW));
        if (crossbowCooldownEnabled) {
            component = component.appendSpace().append(Translations.component("champions.tip.crossbowweapontip.cooldown",
                    Component.text(String.valueOf(crossbowCooldownDuration), NamedTextColor.YELLOW)));
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
