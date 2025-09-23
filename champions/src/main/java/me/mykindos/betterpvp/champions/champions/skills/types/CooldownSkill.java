package me.mykindos.betterpvp.champions.champions.skills.types;

import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.components.champions.IChampionsSkill;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public interface CooldownSkill extends IChampionsSkill {

    double getCooldown(int level);

    default boolean showCooldownFinished() {
        return true;
    }

    default boolean isCancellable(){
        return false;
    }

    /**
     * Tells {@link me.mykindos.betterpvp.champions.champions.skills.listeners.SkillListener} whether to show
     * the cooldown actionbar or not. This is used usually for channeling skills who want to hide the action bar
     * while the player is charging up the skill.
     */
    default boolean shouldDisplayActionBar(Gamer gamer) {
        return isHolding(gamer.getPlayer());
    }

    /**
     * Tells {@link me.mykindos.betterpvp.champions.champions.skills.listeners.SkillListener} that the player is still
     * using the skill based on the return value of this function.
     * <p>
     * This is overridden for channeling skills and stateful skills
     * like Excessive Force where the cooldown shouldn't start until after the effects of the ability have ended.
     */
    default boolean isPlayerCurrentlyUsingSkill(@NotNull Player player) {
        return false;
    }

    default int getPriority() {
        return 1000;
    }

}
