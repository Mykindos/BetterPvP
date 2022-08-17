package me.mykindos.betterpvp.core.combat.log;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.event.Listener;

@Singleton
@BPvPListener
public class DamageLogListener implements Listener {

    private final DamageLogManager damageLogManager;

    @Inject
    public DamageLogListener(DamageLogManager damageLogManager) {
        this.damageLogManager = damageLogManager;
    }

    @UpdateEvent(delay = 100, isAsync = true)
    public void processDamageLogs() {
        damageLogManager.getObjects().forEach((uuid, logs) -> {
            logs.removeIf(log -> log.getExpiry() - System.currentTimeMillis() <= 0);
        });

        damageLogManager.getObjects().entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }
}
