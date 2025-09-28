package me.mykindos.betterpvp.core.framework.patch;

import com.destroystokyo.paper.event.player.PlayerRecipeBookClickEvent;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRecipeBookSettingsChangeEvent;
import org.bukkit.event.player.PlayerRecipeDiscoverEvent;

/**
 * Disables recipe book
 */
@BPvPListener
@Singleton
public class RecipeBookPatch implements Listener {

    @EventHandler
    void onJoin(PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        player.undiscoverRecipes(player.getDiscoveredRecipes());
    }

    @EventHandler
    void onDiscover(PlayerRecipeDiscoverEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    void onSettings(PlayerRecipeBookSettingsChangeEvent event) {
        // ignore
    }

    @EventHandler
    void onClick(PlayerRecipeBookClickEvent event) {
        event.setCancelled(true);
    }

}
