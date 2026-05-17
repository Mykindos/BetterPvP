package me.mykindos.betterpvp.progression.booster;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.components.professions.PlayerProgressionExperienceEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

@Singleton
@BPvPListener
public class BoosterListener implements Listener {

    private final BoosterManager boosterManager;
    private final BoosterRepository repository;

    @Inject
    public BoosterListener(BoosterManager boosterManager, BoosterRepository repository) {
        this.boosterManager = boosterManager;
        this.repository = repository;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onExperienceGain(PlayerProgressionExperienceEvent event) {
        if (boosterManager.hasBooster(event.getPlayer().getUniqueId())) {
            event.setGainedExp(event.getGainedExp() * 1.20);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        repository.getBoosterExpiry(event.getPlayer().getUniqueId()).thenAccept(expiryOpt -> {
            expiryOpt.ifPresent(expiry -> {
                if (expiry > System.currentTimeMillis()) {
                    boosterManager.getActiveBoosters().put(event.getPlayer().getUniqueId(), expiry);
                }
            });
        });
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        boosterManager.getActiveBoosters().remove(event.getPlayer().getUniqueId());
    }
}
