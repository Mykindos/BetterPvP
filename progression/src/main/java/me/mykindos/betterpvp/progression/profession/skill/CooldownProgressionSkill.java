package me.mykindos.betterpvp.progression.profession.skill;

import me.mykindos.betterpvp.core.client.gamer.Gamer;
import org.bukkit.entity.Player;

public interface CooldownProgressionSkill  {
    double getCooldown(int level);

    void whenPlayerUsesSkill(Player player, int level);

    PerkActivator getActivator();

    boolean shouldDisplayActionBar(Gamer gamer);

    default int getPriority() {
        return 1001;
    }
}
