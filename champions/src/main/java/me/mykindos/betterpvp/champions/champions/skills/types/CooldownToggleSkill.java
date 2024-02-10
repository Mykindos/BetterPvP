package me.mykindos.betterpvp.champions.champions.skills.types;

import me.mykindos.betterpvp.champions.champions.skills.data.SkillWeapons;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import org.bukkit.entity.Player;

public interface CooldownToggleSkill extends CooldownSkill, ToggleSkill {

    @Override
    default boolean shouldDisplayActionBar(Gamer gamer) {
        Player player = gamer.getPlayer();

        return player != null && (SkillWeapons.isHolding(player, SkillType.AXE) || SkillWeapons.isHolding(player, SkillType.SWORD));
    }

    @Override
    default int getPriority() {
        return 999;
    }

}
