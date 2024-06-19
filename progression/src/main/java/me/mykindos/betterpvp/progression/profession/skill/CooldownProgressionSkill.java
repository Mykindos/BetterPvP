package me.mykindos.betterpvp.progression.profession.skill;

import org.bukkit.entity.Player;

public interface CooldownProgressionSkill  {
    double getCooldown(int level);

    void whenPlayerUsesSkill(Player player, int level);
}
