package me.mykindos.betterpvp.core.cooldowns;

import com.google.inject.Inject;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.event.Listener;


@BPvPListener
public class CooldownListener implements Listener {

    private final CooldownManager cooldownManager;

    @Inject
    public CooldownListener(CooldownManager cooldownManager) {
        this.cooldownManager = cooldownManager;
    }

    @UpdateEvent(delay = 100, isAsync = true)
    public void processCooldowns() {
        cooldownManager.processCooldowns();
    }
}
