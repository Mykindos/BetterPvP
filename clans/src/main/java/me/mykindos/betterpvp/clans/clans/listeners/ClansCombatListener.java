package me.mykindos.betterpvp.clans.clans.listeners;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.events.ClanKickMemberEvent;
import me.mykindos.betterpvp.clans.clans.events.MemberJoinClanEvent;
import me.mykindos.betterpvp.clans.clans.events.MemberLeaveClanEvent;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.combat.combatlog.events.PlayerCombatLogEvent;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import me.mykindos.betterpvp.core.combat.events.EntityCanHurtEntityEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.Objects;
import java.util.Optional;

@BPvPListener
@Singleton
public class ClansCombatListener implements Listener {

    private final ClanManager clanManager;
    private final ClientManager clientManager;

    @Inject
    public ClansCombatListener(ClanManager clanManager, ClientManager clientManager) {
        this.clanManager = clanManager;
        this.clientManager = clientManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerMountDamage(DamageEvent event) {
        if (event.isCancelled()) return;

        if (event.getDamager() instanceof Player damager) {

            for (Entity passenger : event.getDamagee().getPassengers()) {
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
    public void onPreDamageSafezone(DamageEvent event) {
        if (event.isCancelled()) return;

        if (event.getDamager() instanceof Player damager && event.getDamagee() instanceof Player damagee) {
            if (!clanManager.canHurt(damager, damagee)) {

                UtilMessage.message(damager, "Clans", "You cannot hurt <yellow>%s<gray>.", damagee.getName());
                event.setCancelled(true);
                return;
            }
        }

        if (!event.isDamageeLiving()) return;
        LivingEntity damagee = Objects.requireNonNull(event.getLivingDamagee());
        Optional<Clan> locationClanOptional = clanManager.getClanByLocation(damagee.getLocation());
        if (locationClanOptional.isPresent()) {
            Clan locationClan = locationClanOptional.get();
            if (locationClan.isAdmin() && locationClan.isSafe()) {

                if(damagee instanceof Player damageePlayer && event.getDamager() instanceof Player damagerPlayer) {
                    Clan damageeClan = clanManager.getClanByPlayer(damageePlayer).orElse(null);
                    Clan damagerClan = clanManager.getClanByPlayer(damagerPlayer).orElse(null);
                    if(damageeClan != null && damagerClan != null) {
                        if (clanManager.getPillageHandler().getActivePillages().stream().anyMatch(pillage -> pillage.getPillager().getName().equals(damagerClan.getName())
                                || pillage.getPillaged().getName().equals(damageeClan.getName()))) {
                            return;
                        }
                    }
                }


                if (damagee instanceof Player player) {
                    Gamer gamer = clientManager.search().online(player).getGamer();
                    if (!gamer.isInCombat()) {
                        event.setCancelled(true);
                    }
                } else {
                    event.setCancelled(true);
                }
            }
        }

    }

    @EventHandler
    public void onCanHurt(EntityCanHurtEntityEvent event) {
        if(!event.isAllowed()) {
            return;
        }

        if(event.getDamagee() instanceof Player damagee && event.getDamager() instanceof Player damager){
            if(!clanManager.canHurt(damager, damagee)) {
                event.setResult(Event.Result.DENY);
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

    @EventHandler
    public void onMemberLeaveClan(MemberLeaveClanEvent event) {
        final Client client = clientManager.search().online(event.getPlayer());
        final Gamer gamer = client.getGamer();
        if (gamer.isInCombat()) {
            UtilMessage.message(event.getPlayer(), "Clans", "You cannot leave a clan while in combat!");
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onMemberKickClan(ClanKickMemberEvent event) {
        clientManager.search().online(event.getClanMember().getUuid()).ifPresent(target -> {
            final Gamer gamer = target.getGamer();
            if (gamer.isInCombat()) {
                UtilMessage.message(event.getPlayer(), "Clans", "You cannot kick <yellow>%s</yellow>, they are in combat!", target.getName());
                event.setCancelled(true);
            }
        });


    }

    @EventHandler
    public void onJoinClan(MemberJoinClanEvent event) {
        final Client client = clientManager.search().online(event.getPlayer());
        final Gamer gamer = client.getGamer();
        if (gamer.isInCombat()) {
            UtilMessage.message(event.getPlayer(), "Clans", "You cannot join a clan while in combat!");
            event.setCancelled(true);
        }
    }

}
