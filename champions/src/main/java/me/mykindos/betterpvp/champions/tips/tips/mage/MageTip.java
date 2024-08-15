package me.mykindos.betterpvp.champions.tips.tips.mage;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.tips.ChampionsTip;
import me.mykindos.betterpvp.core.components.champions.Role;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

@Singleton
public class MageTip extends ChampionsTip {

    @Inject
    public MageTip(Champions champions) {
        super(champions, 1, 1, Component.text(Role.MAGE.getDescription()));
    }

    @Override
    public String getName() {
        return "magetip";
    }

    @Override
    public boolean isValid(Player player, Role role) {
        return true;
    }
}
