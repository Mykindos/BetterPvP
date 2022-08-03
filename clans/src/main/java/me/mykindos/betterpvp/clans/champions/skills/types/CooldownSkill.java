package me.mykindos.betterpvp.clans.champions.skills.types;

public interface CooldownSkill {

    int getCooldown(int level);

    default boolean showCooldownFinished() {
        return true;
    }

    default boolean isCancellable(){
        return false;
    }

}
