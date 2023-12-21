package me.mykindos.betterpvp.champions.champions.skills.types;

import me.mykindos.betterpvp.champions.champions.skills.data.SkillWeapons;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.components.champions.ISkill;
import org.bukkit.entity.Player;

public interface CooldownSkill extends ISkill {

    double getCooldown(int level);

    default boolean showCooldownFinished() {
        return true;
    }

    default boolean isCancellable(){
        return false;
    }

    default boolean shouldDisplayActionBar(Gamer gamer) {
        final Player found = gamer.getPlayer();
        if (found == null || !hasSkill(found)) {
            return false;
        }

        return SkillWeapons.getTypeFrom(found.getInventory().getItemInMainHand()) == getType();
    }

}
