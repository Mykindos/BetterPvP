package me.mykindos.betterpvp.clans.clans.fatigue;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.core.combat.damagelog.DamageLog;
import me.mykindos.betterpvp.core.combat.damagelog.DamageLogManager;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.Optional;
import java.util.UUID;

/**
 * Adapter only: turns a Bukkit {@link PlayerDeathEvent} into a {@link DeathContext},
 * hands it to the scoring layer, and triggers the hold if the resolved tier
 * demands it. No scoring, punishment, or presentation logic lives here.
 */
@Singleton
@BPvPListener
public class BattleFatigueListener implements Listener {

    private final ClanManager clanManager;
    private final DamageLogManager damageLogManager;
    private final BattleFatigueManager fatigueManager;
    private final RespawnHoldService respawnHoldService;

    @Inject
    public BattleFatigueListener(ClanManager clanManager,
                                 DamageLogManager damageLogManager,
                                 BattleFatigueManager fatigueManager,
                                 RespawnHoldService respawnHoldService) {
        this.clanManager = clanManager;
        this.damageLogManager = damageLogManager;
        this.fatigueManager = fatigueManager;
        this.respawnHoldService = respawnHoldService;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDeath(PlayerDeathEvent event) {
        final Player player = event.getPlayer();
        final DeathContext context = new DeathContext(
                player.getUniqueId(),
                resolveKiller(player),
                player.getLocation(),
                System.currentTimeMillis(),
                distanceFromSafety(player));

        final FatigueTier tier = fatigueManager.recordDeath(player, context);
        if (tier.requiresHold()) {
            respawnHoldService.beginHold(player, tier);
        }
    }

    private UUID resolveKiller(Player killed) {
        final DamageLog lastDamage = damageLogManager.getLastDamager(killed);
        if (lastDamage != null && lastDamage.getDamager() instanceof Player killer) {
            return killer.getUniqueId();
        }
        return null;
    }

    /**
     * Distance from the death spot to the player's own clan core — a cheap proxy
     * for "how far from home did you recklessly roam". {@code -1} when it can't
     * be measured (no clan, no core, or a different world).
     */
    private double distanceFromSafety(Player player) {
        final Optional<Clan> clanOptional = clanManager.getClanByPlayer(player);
        if (clanOptional.isEmpty()) {
            return -1;
        }
        final Clan clan = clanOptional.get();
        if (clan.getCore() == null || clan.getCore().getPosition() == null) {
            return -1;
        }
        final Location core = clan.getCore().getPosition();
        final Location death = player.getLocation();
        if (core.getWorld() == null || !core.getWorld().equals(death.getWorld())) {
            return -1;
        }
        return core.distance(death);
    }
}
