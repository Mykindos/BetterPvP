package me.mykindos.betterpvp.clans.champions.skills.types;

import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.champions.ChampionsManager;
import me.mykindos.betterpvp.clans.champions.builds.menus.events.SkillDequipEvent;
import me.mykindos.betterpvp.clans.champions.skills.Skill;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;


public abstract class PrepareSkill extends Skill implements InteractSkill, Listener {

    protected final Set<UUID> active = new HashSet<>();

    public PrepareSkill(Clans clans, ChampionsManager championsManager) {
        super(clans, championsManager);
    }

    @EventHandler
    public void onDequip(SkillDequipEvent event) {
        if (event.getSkill() == this) {
            active.remove(event.getPlayer().getUniqueId());
        }
    }

    @Override
    public boolean canUse(Player player) {
        if (active.contains(player.getUniqueId())) {
            UtilMessage.message(player, getClassType().getName(), "You have already prepared %s.",
                    ChatColor.GREEN + getName() + ChatColor.GRAY);
            return false;
        }

        return true;
    }
}
