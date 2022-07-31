package me.mykindos.betterpvp.core.cooldowns;

import com.google.inject.Inject;
import me.mykindos.betterpvp.core.client.events.ClientLoginEvent;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.concurrent.ConcurrentHashMap;

@BPvPListener
public class CooldownListener implements Listener {

    private final CooldownManager cooldownManager;

    @Inject
    public CooldownListener(CooldownManager cooldownManager) {
        this.cooldownManager = cooldownManager;
    }

    @EventHandler
    public void onClientJoin(ClientLoginEvent event) {
        cooldownManager.addObject(event.getPlayer().getUniqueId().toString(), new ConcurrentHashMap<>());
    }

    @UpdateEvent(delay = 100, isAsync = true)
    public void processCooldowns() {
        cooldownManager.processCooldowns();
    }
}
