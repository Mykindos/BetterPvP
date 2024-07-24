package me.mykindos.betterpvp.progression.profession.skill;

import me.mykindos.betterpvp.core.client.gamer.Gamer;
import org.bukkit.entity.Player;

public interface CooldownProgressionSkill  {
    double getCooldown(int level);

    void whenPlayerUsesSkill(Player player, int level);

    PerkActivator getActivator();

    default boolean shouldDisplayActionBar(Gamer gamer) {
        if (getActivator().equals(PerkActivator.AXE)) {
            return gamer.getPlayer().getInventory().getItemInMainHand().getType().name().contains("_AXE");
        }

        return false;
    }

    default int getPriority() {
        return 1001;
    }
}
