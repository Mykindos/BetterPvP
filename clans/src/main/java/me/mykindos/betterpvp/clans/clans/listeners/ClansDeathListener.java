package me.mykindos.betterpvp.clans.clans.listeners;

import com.google.inject.Inject;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.ClanRelation;
import me.mykindos.betterpvp.core.combat.death.events.CustomDeathEvent;
import me.mykindos.betterpvp.core.combat.damagelog.DamageLog;
import me.mykindos.betterpvp.core.combat.damagelog.DamageLogManager;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

@BPvPListener
public class ClansDeathListener implements Listener {

    private final ClanManager clanManager;
    private final DamageLogManager damageLogManager;

    @Inject
    public ClansDeathListener(ClanManager clanManager, DamageLogManager damageLogManager) {
        this.clanManager = clanManager;
        this.damageLogManager = damageLogManager;
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

        if (event.getKiller() instanceof Player killer) {
            Clan killerClan = clanManager.getClanByPlayer(killer).orElse(null);
            ClanRelation relation = clanManager.getRelation(receiverClan, killerClan);
            event.setCustomDeathMessage(event.getCustomDeathMessage().replace(killer.getName(),
                    relation.getPrimaryMiniColor() + killer.getName()));
        }
    }

    @EventHandler
    public void onEnemyPvPDeath(PlayerDeathEvent event) {
        Player killed = event.getPlayer();
        DamageLog lastDamage = damageLogManager.getLastDamager(killed);
        if (lastDamage == null) return;
        if (lastDamage.getDamager() instanceof Player killer) {
            Clan killedClan = clanManager.getClanByPlayer(killed).orElse(null);
            Clan killerClan = clanManager.getClanByPlayer(killer).orElse(null);

            if(killedClan != null & killerClan != null) {
                if (killerClan.isNoDominanceCooldownActive()) {
                    killerClan.messageClan("You did not gain any dominance as your clan is a new clan or was recently pillaged.", null, true);
                    killedClan.messageClan("You did not lose any dominance as <yellow>" + killedClan.getName() + "<gray> is a new clan or was recently pillaged.", null, true);
                    return;
                }

                if (killedClan.isNoDominanceCooldownActive()) {
                    killedClan.messageClan("You did not lose any dominance as your clan is a new clan or was recently pillaged.", null, true);
                    killerClan.messageClan("You did not gain any dominance as <yellow>" + killedClan.getName() + "<gray> is a new clan or was recently pillaged.", null, true);
                    return;
                }
            }


            clanManager.applyDominance(killedClan, killerClan);


        }
    }

    @EventHandler
    public void onEnemyTerritoryDeath(PlayerDeathEvent event) {
        Player killed = event.getPlayer();
        DamageLog lastDamage = damageLogManager.getLastDamager(killed);
        if (lastDamage != null && lastDamage.getDamager() == null) {
            Clan killedClan = clanManager.getClanByPlayer(killed).orElse(null);
            Clan killerClan = clanManager.getClanByLocation(killed.getLocation()).orElse(null);

            if (killerClan != null && killerClan.isOnline()) {

                if(killedClan != null) {
                    if (killerClan.isNoDominanceCooldownActive()) {
                        killerClan.messageClan("You did not gain any dominance as your clan is a new clan or was recently pillaged.", null, true);
                        killedClan.messageClan("You did not lose any dominance as <yellow>" + killedClan.getName() + "<gray> is a new clan or was recently pillaged.", null, true);
                        return;
                    }

                    if (killedClan.isNoDominanceCooldownActive()) {
                        killerClan.messageClan("You did not gain any dominance as <yellow>" + killedClan.getName() + "<gray> is a new clan or was recently pillaged.", null, true);
                        killedClan.messageClan("You did not lose any dominance as your clan is a new clan or was recently pillaged.", null, true);
                        return;
                    }
                }

                clanManager.applyDominance(killedClan, killerClan);
            }
        }
    }
}
