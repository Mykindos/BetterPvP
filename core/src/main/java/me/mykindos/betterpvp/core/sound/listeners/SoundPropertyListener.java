package me.mykindos.betterpvp.core.sound.listeners;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.properties.ClientProperty;
import me.mykindos.betterpvp.core.client.properties.ClientPropertyUpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.sound.SoundManager;
import me.mykindos.betterpvp.core.sound.model.SoundCategory;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Listens for property changes and player disconnects to manage sound playback
 */
@Singleton
@BPvPListener
public class SoundPropertyListener implements Listener {

    private final SoundManager soundManager;

    @Inject
    public SoundPropertyListener(SoundManager soundManager) {
        this.soundManager = soundManager;
    }

    /**
     * Handles client property updates to stop sounds when categories are disabled
     */
    @EventHandler
    public void onClientPropertyUpdate(ClientPropertyUpdateEvent event) {
        String propertyName = event.getProperty();
        Object value = event.getNewValue();

        // Check if sound category property was changed to false
        if (value instanceof Boolean && !(Boolean) value) {
            Player player = Bukkit.getPlayer(event.getContainer().getUniqueId());
            if (player != null && player.isOnline()) {
                SoundCategory category = null;

                if (propertyName.equals(ClientProperty.MUSIC_ENABLED.name())) {
                    category = SoundCategory.MUSIC;
                } else if (propertyName.equals(ClientProperty.AMBIENT_ENABLED.name())) {
                    category = SoundCategory.AMBIENT;
                } else if (propertyName.equals(ClientProperty.UI_SOUNDS_ENABLED.name())) {
                    category = SoundCategory.UI;
                }

                if (category != null) {
                    soundManager.stopSoundsForPlayer(player, category);
                }
            }
        }
    }

    /**
     * Handles player disconnects to clean up sound instances
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        soundManager.cleanupPlayer(event.getPlayer().getUniqueId());
    }

}
