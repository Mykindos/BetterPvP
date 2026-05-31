package me.mykindos.betterpvp.clans.clans.protection;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.zone.ClanZones;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.gamer.properties.GamerProperty;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import me.mykindos.betterpvp.core.world.zone.PlayerEnterZoneEvent;
import me.mykindos.betterpvp.core.world.zone.PlayerExitZoneEvent;
import me.mykindos.betterpvp.core.world.zone.ZoneManager;
import me.mykindos.betterpvp.core.world.zone.Zones;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

@Slf4j
@Singleton
@BPvPListener
public class ClansProtectionListener implements Listener {
    @Inject
    @Config(path = "protection.prevent-non-safe-non-own-territory-entrance", defaultValue = "false")
    private boolean preventNonSafeNonOwnTerritoryEntrance;
    private final ClientManager clientManager;
    private final ClanManager clanManager;
    private final EffectManager effectManager;
    private final ZoneManager zoneManager;

    @Inject
    public ClansProtectionListener(ClientManager clientManager, ClanManager clanManager, EffectManager effectManager, ZoneManager zoneManager) {
        this.clientManager = clientManager;
        this.clanManager = clanManager;
        this.effectManager = effectManager;
        this.zoneManager = zoneManager;
    }

    //if leaving a safezone, resume the protection timer
    @EventHandler
    public void onExitSafeZone(PlayerExitZoneEvent event) {
        if (!event.getZone().hasTag(Zones.SAFE)) return;
        if (!effectManager.hasEffect(event.getPlayer(), EffectTypes.PROTECTION)) return;

        final Gamer gamer = clientManager.search().online(event.getPlayer()).getGamer();
        gamer.setLastSafeNow();
        final long remainingProtection = gamer.getLongProperty(GamerProperty.REMAINING_PVP_PROTECTION);
        if (remainingProtection > 0) {
            UtilServer.runTask(JavaPlugin.getPlugin(Champions.class), () -> {
                effectManager.removeEffect(event.getPlayer(), EffectTypes.PROTECTION, false);
                effectManager.addEffect(event.getPlayer(), EffectTypes.PROTECTION, remainingProtection);
            });
            UtilMessage.message(event.getPlayer(), "Protection", "Protection timer resumed, you have left a safezone.");
            UtilMessage.message(event.getPlayer(), "Protection", "You currently have <green>%s</green> of protection remaining", UtilTime.getTime(remainingProtection, 1));
        }
    }

    //if entering a safezone, pause the protection timer
    @EventHandler
    public void onEnterSafeZone(PlayerEnterZoneEvent event) {
        if (!event.getZone().hasTag(Zones.SAFE)) return;
        if (!effectManager.hasEffect(event.getPlayer(), EffectTypes.PROTECTION)) return;

        final Gamer gamer = clientManager.search().online(event.getPlayer()).getGamer();
        gamer.updateRemainingProtection();
        final long remainingProtection = gamer.getLongProperty(GamerProperty.REMAINING_PVP_PROTECTION);
        if (remainingProtection > 0) {
            UtilMessage.message(event.getPlayer(), "Protection", "Protection timer paused, you have entered a safezone.");
            UtilMessage.message(event.getPlayer(), "Protection", "You currently have <green>%s</green> of protection remaining",
                    UtilTime.getTime(remainingProtection, 1));
            UtilServer.runTask(JavaPlugin.getPlugin(Champions.class), () -> {
                effectManager.removeEffect(event.getPlayer(), EffectTypes.PROTECTION, false);
                effectManager.addEffect(event.getPlayer(), EffectTypes.PROTECTION, 100_000L * 1000L);
            });
        }
    }

    //only allow entrance to own territory while protected
    @EventHandler(priority = EventPriority.LOWEST)
    public void onEnterTerritory(PlayerEnterZoneEvent event) {
        if (!preventNonSafeNonOwnTerritoryEntrance) return;
        if (!event.getZone().hasTag(ClanZones.TERRITORY)) return;

        final Player player = event.getPlayer();
        if (!effectManager.hasEffect(player, EffectTypes.PROTECTION)) return;

        final Clan owner = clanManager.getClanByLocation(player.getLocation()).orElse(null);
        if (owner == null) return;
        final Clan self = clanManager.getClanByPlayer(player).orElse(null);
        if (owner.equals(self)) return;

        final long duration = effectManager.getDuration(player, EffectTypes.PROTECTION);
        UtilMessage.message(player, "Protection", "You cannot enter other territories while protected!");
        UtilMessage.message(player, "Protection", "You currently have <green>%s</green> of protection remaining",
                UtilTime.getTime(duration, 1));
        EffectTypes.disableProtectionReminder(player);
        clanManager.closestWilderness(player).ifPresent(location -> player.teleportAsync(location));
    }

    @UpdateEvent(delay = 240 * 1000L)
    public void protectionReminder() {
        clientManager.getOnline().forEach(client -> {
            final Gamer gamer = client.getGamer();
            final Player player = gamer.getPlayer();

            if (effectManager.hasEffect(player, EffectTypes.PROTECTION)) {
                assert player != null;
                long remainingProtection = gamer.getLongProperty(GamerProperty.REMAINING_PVP_PROTECTION);
                if (!zoneManager.hasTagAt(player.getLocation(), Zones.SAFE)) {
                    remainingProtection = remainingProtection - (System.currentTimeMillis() - gamer.getLastSafe());
                    gamer.updateRemainingProtection();
                }

                if (remainingProtection > 0) {
                    UtilMessage.message(player, "Protection", "You have <green>%s</green> of protection remaining",
                            UtilTime.getTime(remainingProtection, 1));
                }
            }
        });

    }
}
