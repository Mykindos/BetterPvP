package me.mykindos.betterpvp.clans.clans.listeners;

import com.google.inject.Inject;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.combat.combatlog.events.PlayerCombatLogEvent;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.combat.events.PreCustomDamageEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.Optional;

@BPvPListener
public class ClansCombatListener implements Listener {

    private final ClanManager clanManager;
    private final ClientManager clientManager;

    @Inject
    public ClansCombatListener(ClanManager clanManager, ClientManager clientManager) {
        this.clanManager = clanManager;
        this.clientManager = clientManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerMountDamage(PreCustomDamageEvent event) {
        if (event.isCancelled()) return;

        CustomDamageEvent cde = event.getCustomDamageEvent();

        if (cde.getDamager() instanceof Player damager) {

            for (Entity passenger : cde.getDamagee().getPassengers()) {
                if (passenger instanceof Player mountedPlayer) {
                    if (!clanManager.canHurt(damager, mountedPlayer)) {
                        event.setCancelled(true);
                        return;
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPreDamageSafezone(PreCustomDamageEvent event) {
        if (event.isCancelled()) return;

        CustomDamageEvent cde = event.getCustomDamageEvent();
        if (cde.getDamager() instanceof Player damager && cde.getDamagee() instanceof Player damagee) {
            if (!clanManager.canHurt(damager, damagee)) {
                UtilMessage.message(damager, "Clans", "You cannot hurt <yellow>%s<gray>.", damagee.getName());
                //event.setCancelled(true);
                return;
            }
        }

        LivingEntity damagee = cde.getDamagee();
        Optional<Clan> locationClanOptional = clanManager.getClanByLocation(damagee.getLocation());
        if (locationClanOptional.isPresent()) {
            Clan locationClan = locationClanOptional.get();
            if (locationClan.isAdmin() && locationClan.isSafe()) {
                if (damagee instanceof Player player) {
                    Gamer gamer = clientManager.search().online(player).getGamer();
                    if (UtilTime.elapsed(gamer.getLastDamaged(), 15000)) {
                        event.setCancelled(true);
                    }
                } else {
                    event.setCancelled(true);
                }
            }
        }

    }

    @EventHandler
    public void onCombatLog(PlayerCombatLogEvent event) {

        Optional<Clan> clanOptional = clanManager.getClanByLocation(event.getPlayer().getLocation());
        if (clanOptional.isPresent()) {
            Clan locationClan = clanOptional.get();

            Optional<Clan> playerClanOptional = clanManager.getClanByPlayer(event.getPlayer());
            if (playerClanOptional.isPresent()) {
                Clan playerClan = playerClanOptional.get();

                if (!playerClan.equals(locationClan) && !locationClan.isSafe()) {

                    if (playerClan.isEnemy(locationClan)) {
                        event.setSafe(false);
                        event.setDuration(30_000);
                    } else if (!playerClan.isAllied(locationClan)) {
                        event.setSafe(false);
                    }
                }
            } else {
                if (!locationClan.isSafe()) {
                    event.setSafe(false);
                }
            }
        }
    }


}
