package me.mykindos.betterpvp.clans.clans.listeners;

import com.google.inject.Inject;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.combat.events.PreCustomDamageEvent;
import me.mykindos.betterpvp.core.gamer.Gamer;
import me.mykindos.betterpvp.core.gamer.GamerManager;
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
    private final GamerManager gamerManager;

    @Inject
    public ClansCombatListener(ClanManager clanManager, GamerManager gamerManager) {
        this.clanManager = clanManager;
        this.gamerManager = gamerManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerMountDamage(PreCustomDamageEvent event) {
        if (event.isCancelled()) return;

        CustomDamageEvent cde = event.getCustomDamageEvent();

        if (cde.getDamagee() instanceof LivingEntity && cde.getDamager() instanceof Player) {
            LivingEntity damagee = (LivingEntity) cde.getDamagee();
            Player damager = (Player) cde.getDamager();

            for (Entity passenger : damagee.getPassengers()) {
                if (passenger instanceof Player) {
                    Player mountedPlayer = (Player) passenger;
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
                event.setCancelled(true);
                return;
            }
        }

        LivingEntity damagee = cde.getDamagee();
        Optional<Clan> locationClanOptional = clanManager.getClanByLocation(damagee.getLocation());
        if (locationClanOptional.isPresent()) {
            Clan locationClan = locationClanOptional.get();
            if (locationClan.isAdmin() && locationClan.isSafe()) {
                if (damagee instanceof Player player) {
                    Optional<Gamer> gamerOptional = gamerManager.getObject(player.getUniqueId());
                    gamerOptional.ifPresent(gamer -> {
                        if (UtilTime.elapsed(gamer.getLastDamaged(), 15000)) {
                            event.setCancelled(true);
                        }
                    });
                } else {
                    event.setCancelled(true);
                }
            }
        }

    }


}
