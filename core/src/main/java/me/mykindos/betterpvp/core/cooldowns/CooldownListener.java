package me.mykindos.betterpvp.core.cooldowns;

import com.google.inject.Inject;
import me.mykindos.betterpvp.core.combat.death.events.CustomDeathEvent;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
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

    @EventHandler
    public void onDeath(CustomDeathEvent event) {
        if (!(event.getKilled() instanceof Player player)) return;
        cooldownManager.getObject(player.getUniqueId()).ifPresent(cooldowns -> {
            cooldowns.entrySet().removeIf(cooldown -> cooldown.getValue().isRemoveOnDeath());
        });
    }

}
