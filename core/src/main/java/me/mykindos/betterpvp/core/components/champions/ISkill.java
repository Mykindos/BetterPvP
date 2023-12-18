package me.mykindos.betterpvp.core.components.champions;

import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public interface ISkill {

    String getName();

    String[] getDescription(int level);

    Role getClassType();

    SkillType getType();

    default int getMaxLevel() {
        return 5;
    }

    boolean isEnabled();

    boolean canUseWhileSlowed();

    boolean canUseWhileStunned();

    boolean canUseWhileSilenced();

    boolean canUseWhileLevitating();

    boolean canUseInLiquid();

    default boolean canUse(Player player) {
        return true;
    }

    void loadConfig();

    default boolean displayWhenUsed() {
        return true;
    }

    default boolean ignoreNegativeEffects() {
        return false;
    }

    Material[] getItemsBySkillType();

    boolean hasSkill(Player player);

}
