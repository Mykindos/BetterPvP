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

    default int getPriority() {
        return 1000;
    }

}
