package me.mykindos.betterpvp.clans.clans.listeners;

import com.google.inject.Inject;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.ClanRelation;
import me.mykindos.betterpvp.core.combat.death.events.CustomDeathEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

@BPvPListener
public class ClansDeathListener implements Listener {

    private final ClanManager clanManager;

    @Inject
    public ClansDeathListener(ClanManager clanManager) {
        this.clanManager = clanManager;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDeath(CustomDeathEvent event) {
        Player receiver = event.getReceiver();
        Clan receiverClan = clanManager.getClanByPlayer(receiver).orElse(null);
        if (event.getKilled() instanceof Player killed) {
            Clan killedClan = clanManager.getClanByPlayer(killed).orElse(null);
            ClanRelation relation = clanManager.getRelation(receiverClan, killedClan);
            event.setCustomDeathMessage(event.getCustomDeathMessage().replace(killed.getName(),
                    relation.getPrimaryMiniColor() + killed.getName()));
        }

        if(event.getKiller() instanceof Player killer) {
            Clan killerClan = clanManager.getClanByPlayer(killer).orElse(null);
            ClanRelation relation = clanManager.getRelation(receiverClan, killerClan);
            event.setCustomDeathMessage(event.getCustomDeathMessage().replace(killer.getName(),
                    relation.getPrimaryMiniColor() + killer.getName()));
        }
    }
}
