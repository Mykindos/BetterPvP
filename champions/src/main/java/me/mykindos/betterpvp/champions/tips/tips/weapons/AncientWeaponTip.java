package me.mykindos.betterpvp.champions.tips.tips.weapons;

import com.google.inject.Inject;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.tips.ChampionsTip;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.locale.Translations;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

public class AncientWeaponTip extends ChampionsTip {
    @Inject
    public AncientWeaponTip(Champions champions) {
        super(champions, 1, 1, Translations.component("champions.tip.ancientweapontip",
                Component.text("Ancient", NamedTextColor.DARK_PURPLE),
                Component.text("Netherite", NamedTextColor.DARK_PURPLE),
                Component.text("booster", NamedTextColor.GOLD),
                Component.text("power", NamedTextColor.AQUA),
                Component.text("1", NamedTextColor.YELLOW),
                Component.text("1", NamedTextColor.YELLOW)));
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
