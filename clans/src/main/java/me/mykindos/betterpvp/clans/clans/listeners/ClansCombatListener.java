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
import me.mykindos.betterpvp.core.combat.events.CustomEntityVelocityEvent;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import me.mykindos.betterpvp.core.combat.events.EntityCanHurtEntityEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
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
        handleSafezone(event, Objects.requireNonNull(event.getLivingDamagee()), event.getDamager());
    }

    @EventHandler(ignoreCancelled = true)
    public void onVelocity(CustomEntityVelocityEvent event) {
        if (event.getSource() == event.getEntity()) return;
        if (!(event.getEntity() instanceof LivingEntity livingEntity)) return;
        handleSafezone(event, livingEntity, event.getSource());
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

    /**
     * Shared safezone logic: cancels {@code event} if the location is safe and the
     * acting entity is not in combat, unless the two players are involved in an active pillage.
     */
    private void handleSafezone(Cancellable event, LivingEntity acted, Entity source) {
        Optional<Clan> locationClanOptional = clanManager.getClanByLocation(acted.getLocation());
        if (locationClanOptional.isEmpty()) return;
        if (!locationClanOptional.get().isSafe()) return;

        if (acted instanceof Player actedPlayer && source instanceof Player sourcePlayer) {
            if (playersInActivePillage(sourcePlayer, actedPlayer)) {
                return;
            }
        }

        cancelIfNotInCombat(event, acted);
    }

    /**
     * Returns true if the two players' clans are involved in an active pillage
     * (source as pillager, acted as pillaged). Returns false if either player has no clan.
     */
    private boolean playersInActivePillage(Player source, Player acted) {
        Clan sourceClan = clanManager.getClanByPlayer(source).orElse(null);
        Clan actedClan = clanManager.getClanByPlayer(acted).orElse(null);
        return sourceClan != null && actedClan != null && isInActivePillage(sourceClan, actedClan);
    }

    /**
     * Returns true if the pillager clan is actively pillaging the pillaged clan.
     */
    private boolean isInActivePillage(Clan pillager, Clan pillaged) {
        return clanManager.getPillageHandler().getActivePillages().stream()
                .anyMatch(pillage -> pillage.getPillager().getName().equals(pillager.getName())
                        || pillage.getPillaged().getName().equals(pillaged.getName()));
    }

    /**
     * Cancels {@code event} if {@code entity} is not a Player currently in combat,
     * or if {@code entity} is not a Player at all.
     */
    private void cancelIfNotInCombat(Cancellable event, LivingEntity entity) {
        if (entity instanceof Player player) {
            Gamer gamer = clientManager.search().online(player).getGamer();
            if (!gamer.isInCombat()) {
                event.setCancelled(true);
            }
        } else {
            event.setCancelled(true);
        }
    }

}
