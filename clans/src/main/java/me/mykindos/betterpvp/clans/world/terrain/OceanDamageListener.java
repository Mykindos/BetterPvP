package me.mykindos.betterpvp.clans.world.terrain;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.combat.cause.EnvironmentalDamageCause;
import me.mykindos.betterpvp.core.combat.cause.VanillaDamageCause;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilDamage;
import me.mykindos.betterpvp.core.world.zone.ZoneManager;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

/**
 * Hurts players who swim in open ocean, in any world. Damage is gated on two independent facts — the player is actually
 * in water, and the location carries the {@link TerrainZones#OCEAN ocean} zone tag — so interior water (ponds, wells,
 * moats the ocean flood-fill never reached) is left safe. The damage amount is resolved per world from
 * {@link TerrainConfig}, so different worlds can be more or less punishing. Administrating staff and creative/spectator
 * players are exempt.
 * <p>
 * This is a per-tick environmental effect, not a combat rule, so it lives on the updater rather than the zone rule
 * system, mirroring how {@code VoidWorldListener} guards its world.
 */
@BPvPListener
@Singleton
public class OceanDamageListener implements Listener {

    private final Clans clans;
    private final ZoneManager zoneManager;
    private final ClientManager clientManager;

    @Inject
    public OceanDamageListener(Clans clans, ZoneManager zoneManager, ClientManager clientManager) {
        this.clans = clans;
        this.zoneManager = zoneManager;
        this.clientManager = clientManager;
    }

    @UpdateEvent(delay = 500)
    public void damageOceanSwimmers() {
        if (!zoneManager.isActive()) {
            return;
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
                continue;
            }
            if (clientManager.search().online(player).isAdministrating()) {
                continue;
            }
            if (!UtilBlock.isInWater(player) || !zoneManager.hasTagAt(player.getLocation(), TerrainZones.OCEAN)) {
                continue;
            }

            final double damage = TerrainConfig.oceanDamage(clans.getConfig(), player.getWorld().getName());
            final EnvironmentalDamageCause cause = new EnvironmentalDamageCause("Ocean",
                    "Ocean",
                    EntityDamageEvent.DamageCause.DROWNING,
                    true,
                    0,
                    false);
            final DamageEvent event = new DamageEvent(player, null, null, cause, damage);
            event.setDamageDelay(0); // the update interval paces the damage; don't also throttle by cause delay
            UtilDamage.doDamage(event);
        }
    }
}
