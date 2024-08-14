package me.mykindos.betterpvp.clans.clans.protection;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.events.PlayerChangeTerritoryEvent;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.gamer.properties.GamerProperty;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

@Slf4j
@Singleton
@BPvPListener
public class ProtectionListener implements Listener {
    private final ClientManager clientManager;
    private final EffectManager effectManager;

    private final ClanManager clanManger;

    @Inject
    public ProtectionListener(ClientManager clientManager, EffectManager effectManager, ClanManager clanManger) {
        this.clientManager = clientManager;
        this.effectManager = effectManager;
        this.clanManger = clanManger;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChangeTerritory(PlayerChangeTerritoryEvent event) {
        if (!effectManager.hasEffect(event.getPlayer(), EffectTypes.PROTECTION)) return;
        Client client = clientManager.search().online(event.getPlayer());
        Gamer gamer = client.getGamer();

        //if leaving safe clan, re-add protection
        if (event.getFromClan() != null && event.getFromClan().isSafe()) {
            gamer.setLastSafeNow();
            long remainingProtection = gamer.getLongProperty(GamerProperty.REMAINING_PVP_PROTECTION);
            if (remainingProtection > 0) {
                UtilServer.runTask(JavaPlugin.getPlugin(Champions.class), () -> {
                    effectManager.removeEffect(event.getPlayer(), EffectTypes.PROTECTION, false);
                    effectManager.addEffect(event.getPlayer(), EffectTypes.PROTECTION, remainingProtection);
                });
                UtilMessage.message(event.getPlayer(), "Protection", "Protection timer resumed, you have left a safezone.");
                UtilMessage.message(event.getPlayer(), "Protection", "You currently have <green>%s</green> of protection remaining", UtilTime.getTime(remainingProtection, 1));
            }

        }
        if (event.getToClan() == null) {
            return;
        }
        //if entering safe clan, pause protection timer
        if (event.getToClan().isSafe()) {
            gamer.updateRemainingProtection();
            long remainingProtection = gamer.getLongProperty(GamerProperty.REMAINING_PVP_PROTECTION);
            if (remainingProtection > 0) {
                UtilMessage.message(event.getPlayer(), "Protection", "Protection timer paused, you have entered a safezone.");
                UtilMessage.message(event.getPlayer(), "Protection", "You currently have <green>%s</green> of protection remaining",
                        UtilTime.getTime(gamer.getLongProperty(GamerProperty.REMAINING_PVP_PROTECTION), 1));
                UtilServer.runTask(JavaPlugin.getPlugin(Champions.class), () -> {
                    effectManager.removeEffect(event.getPlayer(), EffectTypes.PROTECTION, false);
                    effectManager.addEffect(event.getPlayer(), EffectTypes.PROTECTION, 100_000L*1000L);
                });
            }
        }
        //only allow entrance to own territory or admin clan territory
        if (!event.getToClan().equals(event.getClan()) && !event.getToClan().isAdmin()) {
            event.getPlayerMoveEvent().setCancelled(true);
            event.setCancelled(true);
            long duration = effectManager.getDuration(event.getPlayer(), EffectTypes.PROTECTION);
            UtilMessage.message(event.getPlayer(), "Protection", "You cannot enter other territories while protected!");
            UtilMessage.message(event.getPlayer(), "Protection", "You currently have <green>%s</green> of protection remaining",
                    UtilTime.getTime(duration, 1));
            EffectTypes.disableProtectionReminder(event.getPlayer());
            event.getPlayer().teleportAsync(event.getPlayerMoveEvent().getFrom());
        }
    }

    @UpdateEvent(delay = 240 * 1000L)
    public void protectionReminder() {
        clientManager.getOnline().forEach(client -> {
            Gamer gamer = client.getGamer();
            Player player = gamer.getPlayer();

            if (effectManager.hasEffect(player, EffectTypes.PROTECTION)) {
                assert player != null;
                Clan clan = clanManger.getClanByLocation(player.getLocation()).orElse(null);
                long remainingProtection = client.getGamer().getLongProperty(GamerProperty.REMAINING_PVP_PROTECTION);
                if (clan == null || !clan.isSafe()) {
                    remainingProtection = remainingProtection - (System.currentTimeMillis() - gamer.getLastSafe());
                }
                UtilMessage.message(player, "Protection", "You have <green>%s</green> of protection remaining",
                        UtilTime.getTime(remainingProtection, 1));
            }
        });

    }
}
