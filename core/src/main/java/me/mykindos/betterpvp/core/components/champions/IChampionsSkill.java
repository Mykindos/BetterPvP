package me.mykindos.betterpvp.core.components.champions;

import me.mykindos.betterpvp.core.skill.ISkill;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import java.lang.reflect.Method;
import java.util.Arrays;

public interface IChampionsSkill extends ISkill {

    Role getClassType();

    /**
     * The player-facing, translatable display name of this skill, resolved per-viewer at render time from
     * {@code champions.skill.<role>.<skill>.name}. {@link #getName()} remains the stable internal identifier
     * and must not be used for display.
     *
     * @return the translatable display-name component
     */
    default Component getDisplayName() {
        final String classPart = getClassType() != null ? getClassType().name().toLowerCase() : "global";
        final String skillPart = getName().toLowerCase().replace(" ", "-");
        return Component.translatable("champions.skill." + classPart + "." + skillPart + ".name");
    }

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

    Component[] getDescription(int level);

}
