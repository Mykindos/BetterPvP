package me.mykindos.betterpvp.clans.champions.skills;

import me.mykindos.betterpvp.clans.champions.roles.Role;
import me.mykindos.betterpvp.clans.champions.skills.data.SkillType;
import org.bukkit.entity.Player;

public interface ISkill {

    String getName();

    String[] getDescription(int level);

    Role getClassType();

    SkillType getSkillType();

    boolean isEnabled();

    default boolean canUse(Player player) {
        return true;
    }

}
