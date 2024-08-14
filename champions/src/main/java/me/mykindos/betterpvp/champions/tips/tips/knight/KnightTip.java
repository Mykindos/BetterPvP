package me.mykindos.betterpvp.champions.tips.tips.knight;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.tips.ChampionsTip;
import me.mykindos.betterpvp.core.components.champions.Role;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

@Singleton
public class KnightTip extends ChampionsTip {

    @Inject
    public KnightTip(Champions champions) {
        super(champions, 1, 1, Component.text(Role.KNIGHT.getDescription()));
    }

    @Override
    public String getName() {
        return "knighttip";
    }

    @Override
    public boolean isValid(Player player, Role role) {
        return true;
    }
}
