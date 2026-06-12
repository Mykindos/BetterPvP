package me.mykindos.betterpvp.champions.tips.tips.knight;

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
public class DefensiveStanceTip extends ChampionsTip {

    @Inject
    public DefensiveStanceTip(Champions champions) {
        super(champions, 1, 1, Translations.component("champions.tip.defensivestancetip",
                Role.KNIGHT.getDisplayName().color(Role.KNIGHT.getColor()),
                Component.text("Defensive Stance", NamedTextColor.WHITE)));
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
