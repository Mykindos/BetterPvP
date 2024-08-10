package me.mykindos.betterpvp.clans.clans.protection;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.clans.clans.events.PlayerChangeTerritoryEvent;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.gamer.properties.GamerProperty;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.UtilTime;
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

    @Inject
    public ProtectionListener(ClientManager clientManager, EffectManager effectManager) {
        this.clientManager = clientManager;
        this.effectManager = effectManager;
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
                UtilServer.runTask(JavaPlugin.getPlugin(Champions.class), () -> effectManager.addEffect(event.getPlayer(), EffectTypes.PROTECTION, remainingProtection));
            }
            UtilMessage.message(event.getPlayer(), "Protection", "Protection timer resumed, you have left a safezone.");
            UtilMessage.message(event.getPlayer(), "Protection", "You currently have <green>%s<green> of protection remaining", UtilTime.getTime(remainingProtection, 1));
        }
        if (event.getToClan() == null) {
            return;
        }
        //if entering safe clan, pause protection timer
        if (event.getToClan().isSafe()) {
            gamer.updateRemainingProtection();
            UtilMessage.message(event.getPlayer(), "Protection", "Protection timer paused, you have entered a safezone.");
        }
        //only allow entrance to own territory or admin clan territory
        if (!event.getToClan().equals(event.getClan()) && !event.getToClan().isAdmin()) {
            event.getPlayerMoveEvent().setCancelled(true);
            event.setCancelled(true);
            UtilMessage.message(event.getPlayer(), "Protected", "You cannot enter other territories while protected!");
            EffectTypes.disableProtectionReminder(event.getPlayer());
            UtilServer.runTask(JavaPlugin.getPlugin(Champions.class), () -> event.getPlayer().teleport(event.getPlayerMoveEvent().getFrom()));
        }
    }
}
