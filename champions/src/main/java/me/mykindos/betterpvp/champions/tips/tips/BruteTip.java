package me.mykindos.betterpvp.champions.tips.tips;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.tips.ChampionsTip;
import me.mykindos.betterpvp.core.components.champions.Role;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

@Singleton
public class BruteTip extends ChampionsTip {

    @Inject
    public BruteTip(Champions champions) {
        super(champions, 1, 1, Component.text(Role.BRUTE.getDescription()));
    }

    @Override
    public String getName() {
        return "brutetip";
    }

    @Override
    public boolean isValid(Player player, Role role) {
        return true;
    }
}
