package me.mykindos.betterpvp.champions.champions.skills.types;

import me.mykindos.betterpvp.core.components.champions.ISkill;
import me.mykindos.betterpvp.core.gamer.Gamer;
import org.bukkit.entity.Player;

import java.util.List;

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
        if (found == null) {
            return false;
        }

        return List.of(getItemsBySkillType()).contains(found.getInventory().getItemInMainHand().getType());
    }

}
