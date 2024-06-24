package me.mykindos.betterpvp.core.components.champions;

import me.mykindos.betterpvp.core.skill.ISkill;
import org.bukkit.entity.Player;

public interface IChampionsSkill extends ISkill {

    Role getClassType();

    SkillType getType();

    boolean canUseWhileSlowed();

    boolean canUseWhileStunned();

    boolean canUseWhileSilenced();

    boolean canUseWhileLevitating();

    boolean canUseInLiquid();

    default boolean canUse(Player player) {
        return true;
    }

    default boolean displayWhenUsed() {
        return true;
    }

    default boolean ignoreNegativeEffects() {
        return false;
    }

    boolean hasSkill(Player player);

    boolean isHolding(Player player);

}
