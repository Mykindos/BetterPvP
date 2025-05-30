package me.mykindos.betterpvp.clans.clans.listeners;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.ClanRelation;
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
public class ClansDeathListener implements Listener {

    private final ClanManager clanManager;
    private final DamageLogManager damageLogManager;

    @Inject
    @Config(path = "clans.pillage.protection", defaultValue = "true")
    private boolean pillageProtection;

    @Inject
    public ClansDeathListener(ClanManager clanManager, DamageLogManager damageLogManager) {
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

        handleKill(killedClan, killerClan, true);
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

            handleKill(killedClan, killerClan, true);
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
                handleKill(killedClan, killerClan, false);
            }
        }
    }

    private void handleKill(@Nullable Clan killedClan, @Nullable Clan killerClan, boolean allowPillage) {

        if (killedClan == null || killerClan == null) return;
        if (clanManager.getRelation(killedClan, killerClan) != ClanRelation.ENEMY) return;

        if (killerClan.isNoDominanceCooldownActive() && pillageProtection) {
            killerClan.messageClan("You did not gain any dominance as your clan is a new clan or was recently pillaged.", null, true);
            killedClan.messageClan("You did not lose any dominance as <yellow>" + killerClan.getName() + "<gray> is a new clan or was recently pillaged.", null, true);
            return;
        }

        if (killedClan.isNoDominanceCooldownActive() && pillageProtection) {
            killedClan.messageClan("You did not lose any dominance as your clan is a new clan or was recently pillaged.", null, true);
            killerClan.messageClan("You did not gain any dominance as <yellow>" + killedClan.getName() + "<gray> is a new clan or was recently pillaged.", null, true);
            return;
        }

        if (!allowPillage) {
            int killerSize = killerClan.getMembers().size();
            int killedSize = killedClan.getMembers().size();

            double dominance = clanManager.getDominanceForKill(killedSize, killerSize);
            Optional<ClanEnemy> enemyOptional = killerClan.getEnemy(killedClan);
            if (enemyOptional.isPresent()) {
                ClanEnemy enemy = enemyOptional.get();
                if (enemy.getDominance() + dominance >= 100) {
                    return; // Stop starting a raid via suicide in territory
                }
            }
        }

        clanManager.applyDominance(killedClan, killerClan);
    }

}
