package me.mykindos.betterpvp.clans.clans.listeners;

import com.google.inject.Inject;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.ClanRelation;
import me.mykindos.betterpvp.core.combat.damagelog.DamageLog;
import me.mykindos.betterpvp.core.combat.damagelog.DamageLogManager;
import me.mykindos.betterpvp.core.combat.death.events.CustomDeathEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.function.Function;

@BPvPListener
public class ClansDeathListener implements Listener {

    private final ClanManager clanManager;
    private final DamageLogManager damageLogManager;

    @Inject
    public ClansDeathListener(ClanManager clanManager, DamageLogManager damageLogManager) {
        this.clanManager = clanManager;
        this.damageLogManager = damageLogManager;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onDeath(CustomDeathEvent event) {
        final Player receiver = event.getReceiver();
        Clan receiverClan = clanManager.getClanByPlayer(receiver).orElse(null);

        final Function<LivingEntity, Component> def = event.getNameFormat();
        event.setNameFormat(entity -> {
            Component name = def.apply(entity);
            if (entity instanceof Player player) {
                Clan playerClan = clanManager.getClanByPlayer(player).orElse(null);
                ClanRelation relation = clanManager.getRelation(receiverClan, playerClan);
                name = name.color(relation.getPrimary());
            }
            return name;
        });
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
