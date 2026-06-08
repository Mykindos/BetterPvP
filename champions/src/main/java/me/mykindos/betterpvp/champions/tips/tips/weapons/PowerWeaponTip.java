package me.mykindos.betterpvp.champions.tips.tips.weapons;

import com.google.inject.Inject;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.tips.ChampionsTip;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.locale.Translations;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

public class PowerWeaponTip extends ChampionsTip {
    @Inject
    public PowerWeaponTip(Champions champions) {
        super(champions, 1, 1, Translations.component("champions.tip.powerweapontip",
                Component.text("Power", NamedTextColor.AQUA),
                Component.text("Diamond", NamedTextColor.AQUA),
                Component.text("1", NamedTextColor.YELLOW)));
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
