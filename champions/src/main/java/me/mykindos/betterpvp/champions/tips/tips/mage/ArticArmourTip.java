package me.mykindos.betterpvp.champions.tips.tips.mage;

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
public class ArticArmourTip extends ChampionsTip {

    @Inject
    public ArticArmourTip(Champions champions) {
        super(champions, 1, 1, Translations.component("champions.tip.arcticarmourtip",
                Role.MAGE.getDisplayName().color(Role.MAGE.getColor()),
                Component.text("Arctic Armour", NamedTextColor.WHITE),
                Component.text("Resistance", NamedTextColor.WHITE),
                Component.text("slowness", NamedTextColor.WHITE),
                Component.text("freeze", NamedTextColor.WHITE)));
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
