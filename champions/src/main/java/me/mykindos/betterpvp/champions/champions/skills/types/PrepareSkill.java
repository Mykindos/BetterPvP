package me.mykindos.betterpvp.champions.champions.skills.types;

import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.builds.menus.events.SkillDequipEvent;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public abstract class PrepareSkill extends Skill implements InteractSkill, Listener {

    protected final Set<UUID> active = new HashSet<>();

    public PrepareSkill(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @EventHandler
    public void onDequip(SkillDequipEvent event) {
        if (event.getBuildSkill().getSkill() == this) {
            active.remove(event.getPlayer().getUniqueId());
        }
    }

    @Override
    public boolean canUse(Player player) {
        if (active.contains(player.getUniqueId())) {
            UtilMessage.message(player, getClassType().getDisplayName(), "champions.skill.already-prepared", getDisplayName().color(NamedTextColor.GREEN));
            return false;
        }

        return true;
    }
}
