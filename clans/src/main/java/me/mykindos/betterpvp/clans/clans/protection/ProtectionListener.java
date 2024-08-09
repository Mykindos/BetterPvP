package me.mykindos.betterpvp.clans.clans.protection;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.clans.clans.events.PlayerChangeTerritoryEvent;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.events.ClientJoinEvent;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.gamer.properties.GamerProperty;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilServer;
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

    @EventHandler
    public void onLogin(ClientJoinEvent event) {
        long remainingProtection = event.getClient().getGamer().getLongProperty(GamerProperty.REMAINING_PVP_PROTECTION);
        if (remainingProtection > 0) {
            effectManager.addEffect(event.getPlayer(), EffectTypes.PROTECTION, remainingProtection);
        }
        event.getClient().getGamer().setLastSafeNow();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChangeTerritory(PlayerChangeTerritoryEvent event) {
        Client client = clientManager.search().online(event.getPlayer());
        Gamer gamer = client.getGamer();
        //if leaving safe clan, re-add protection
        if (event.getFromClan() != null && event.getFromClan().isSafe()) {
            gamer.setLastSafeNow();
            long remainingProtection = gamer.getLongProperty(GamerProperty.REMAINING_PVP_PROTECTION);
            if (remainingProtection > 0) {
                effectManager.addEffect(event.getPlayer(), EffectTypes.PROTECTION, remainingProtection);
            }
        }
        if (event.getToClan() == null) {
            return;
        }
        //if entering safe clan, pause protection timer
        if (event.getToClan().isSafe()) {
            long remainingProtection = gamer.getLongProperty(GamerProperty.REMAINING_PVP_PROTECTION);
            remainingProtection = remainingProtection - (System.currentTimeMillis() - gamer.getLastSafe());
            gamer.saveProperty(GamerProperty.REMAINING_PVP_PROTECTION, remainingProtection);
        }
        //only allow entrance to own territory or admin clan territory
        if (!event.getToClan().equals(event.getClan()) /*&& !event.getToClan().isAdmin()*/) {
            event.getPlayerMoveEvent().setCancelled(true);
            event.setCancelled(true);
            UtilServer.runTask(JavaPlugin.getPlugin(Champions.class), () -> event.getPlayer().teleport(event.getPlayerMoveEvent().getFrom()));
        }
    }
}
