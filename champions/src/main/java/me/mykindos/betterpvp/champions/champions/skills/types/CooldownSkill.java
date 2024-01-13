package me.mykindos.betterpvp.champions.champions.skills.types;

import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.components.champions.ISkill;
import me.mykindos.betterpvp.core.components.champions.SkillType;

public interface CooldownSkill extends ISkill {

    double getCooldown(int level);

    default boolean showCooldownFinished() {
        return true;
    }

    default boolean isCancellable(){
        return false;
    }

    default boolean shouldDisplayActionBar(Gamer gamer) {
        return isHolding(gamer.getPlayer()) && (getType() == SkillType.AXE || getType() == SkillType.SWORD);
    }

}
