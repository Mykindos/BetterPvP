package me.mykindos.betterpvp.clans.champions.skills.types;

import me.mykindos.betterpvp.clans.champions.skills.ISkill;

public interface CooldownSkill extends ISkill {

    double getCooldown(int level);

    default boolean showCooldownFinished() {
        return true;
    }

    default boolean isCancellable(){
        return false;
    }

}
