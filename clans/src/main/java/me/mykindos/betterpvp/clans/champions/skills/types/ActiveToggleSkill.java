package me.mykindos.betterpvp.clans.champions.skills.types;

import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.champions.ChampionsManager;
import me.mykindos.betterpvp.clans.champions.skills.Skill;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public abstract class ActiveToggleSkill extends Skill implements ToggleSkill, Listener {

    protected final Set<UUID> active = new HashSet<>();

    public ActiveToggleSkill(Clans clans, ChampionsManager championsManager) {
        super(clans, championsManager);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        active.remove(event.getEntity().getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        active.remove(event.getPlayer().getUniqueId());
    }

}
