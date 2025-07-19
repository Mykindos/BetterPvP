package me.mykindos.betterpvp.clans.clans.listeners;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.ClanRelation;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.client.stats.impl.ClientStat;
import me.mykindos.betterpvp.core.combat.combatlog.events.PlayerClickCombatLogEvent;
import me.mykindos.betterpvp.core.combat.damagelog.DamageLog;
import me.mykindos.betterpvp.core.combat.damagelog.DamageLogManager;
import me.mykindos.betterpvp.core.combat.death.events.CustomDeathMessageEvent;
import me.mykindos.betterpvp.core.components.clans.data.ClanEnemy;
import me.mykindos.betterpvp.core.components.clans.events.ClanAddExperienceEvent;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

@BPvPListener
@Singleton
public class ClansDeathListener extends ClanListener implements Listener {

    private final ClanManager clanManager;
    private final DamageLogManager damageLogManager;

    @Inject
    @Config(path = "clans.pillage.protection", defaultValue = "true")
    private boolean pillageProtection;

    @Inject
    public ClansDeathListener(ClanManager clanManager, ClientManager clientManager, DamageLogManager damageLogManager) {
        super(clanManager, clientManager);
        this.clanManager = clanManager;
        this.damageLogManager = damageLogManager;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onDeath(CustomDeathMessageEvent event) {
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

    @EventHandler(priority = EventPriority.HIGH)
    public void onSheepClick(PlayerClickCombatLogEvent event) {
        UUID killer = event.getPlayer().getUniqueId();
        UUID victim = event.getCombatLog().getOwner();
        Clan killedClan = clanManager.getClanByPlayer(victim).orElse(null);
        Clan killerClan = clanManager.getClanByPlayer(killer).orElse(null);

        UtilServer.callEvent(new ClanAddExperienceEvent(event.getPlayer(), 0.2));
        final Client killerClient = clientManager.search().online(event.getPlayer());

        double dominance = handleKill(null, killedClan, killerClient, killerClan, true);
        clientManager.search().offline(victim).thenAccept(killedOptional -> {
            killedOptional.ifPresent(client -> client.getStatContainer().incrementStat(ClientStat.CLANS_DOMINANCE_LOST, dominance));
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

            UtilServer.callEvent(new ClanAddExperienceEvent(killer, 0.2));

            final Client killedClient = clientManager.search().online(killed);
            final Client killerClient = clientManager.search().online(killer);

            handleKill(killedClient, killedClan, killerClient, killerClan, true);
        }
    }

    @EventHandler
    public void onEnemyTerritoryDeath(PlayerDeathEvent event) {
        Player killed = event.getPlayer();
        DamageLog lastDamage = damageLogManager.getLastDamager(killed);
        if (lastDamage == null || lastDamage.getDamager() == null) {
            Clan killedClan = clanManager.getClanByPlayer(killed).orElse(null);
            Clan killerClan = clanManager.getClanByLocation(killed.getLocation()).orElse(null);

            if (killerClan != null && killerClan.isOnline()) {
                handleKill(clientManager.search().online(killed), killedClan, null, killerClan, false);
            }
        }
    }

    private double handleKill(@Nullable Client killed, @Nullable Clan killedClan, @Nullable Client killer, @Nullable Clan killerClan, boolean allowPillage) {

        if (killedClan == null || killerClan == null) return 0;
        if (clanManager.getRelation(killedClan, killerClan) != ClanRelation.ENEMY) return 0;

        if (killerClan.isNoDominanceCooldownActive() && pillageProtection) {
            killerClan.messageClan("You did not gain any dominance as your clan is a new clan or was recently pillaged.", null, true);
            killedClan.messageClan("You did not lose any dominance as <yellow>" + killerClan.getName() + "<gray> is a new clan or was recently pillaged.", null, true);
            return 0;
        }

        if (killedClan.isNoDominanceCooldownActive() && pillageProtection) {
            killedClan.messageClan("You did not lose any dominance as your clan is a new clan or was recently pillaged.", null, true);
            killerClan.messageClan("You did not gain any dominance as <yellow>" + killedClan.getName() + "<gray> is a new clan or was recently pillaged.", null, true);
            return 0;
        }

        if (!allowPillage) {
            int killerSize = killerClan.getMembers().size();
            int killedSize = killedClan.getMembers().size();

            double dominance = clanManager.getDominanceForKill(killedSize, killerSize);
            Optional<ClanEnemy> enemyOptional = killerClan.getEnemy(killedClan);
            if (enemyOptional.isPresent()) {
                ClanEnemy enemy = enemyOptional.get();
                if (enemy.getDominance() + dominance >= 100) {
                    return 0; // Stop starting a raid via suicide in territory
                }
            }
        }

        double dominance = clanManager.applyDominance(killedClan, killerClan);
        if (killer != null) {
            killer.getStatContainer().incrementStat(ClientStat.CLANS_DOMINANCE_GAINED, dominance);
        }
        if (killed != null) {
            killed.getStatContainer().incrementStat(ClientStat.CLANS_DOMINANCE_LOST, dominance);
        }
        return dominance;
    }

}
