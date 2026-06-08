package me.mykindos.betterpvp.champions.tips.tips.weapons;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.tips.ChampionsTip;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.locale.Translations;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

@Singleton
public class BoosterWeaponTip extends ChampionsTip {

    @Inject
    public BoosterWeaponTip(Champions champions) {
        super(champions, 1, 1, Translations.component("champions.tip.boosterweapontip",
                Component.text("Booster", NamedTextColor.GOLD),
                Component.text("Gold", NamedTextColor.GOLD),
                Component.text("1", NamedTextColor.YELLOW)));
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
