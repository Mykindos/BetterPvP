package me.mykindos.betterpvp.champions.champions.skills.types;

import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.components.champions.IChampionsSkill;

public interface CooldownSkill extends IChampionsSkill {

    double getCooldown(int level);

    default boolean showCooldownFinished() {
        return true;
    }

    default boolean isCancellable(){
        return false;
    }

    default boolean shouldDisplayActionBar(Gamer gamer) {
        return isHolding(gamer.getPlayer());
    }

    default int getPriority() {
        return 1000;
    }

}
