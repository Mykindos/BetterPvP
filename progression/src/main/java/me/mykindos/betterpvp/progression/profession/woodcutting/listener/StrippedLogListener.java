package me.mykindos.betterpvp.progression.profession.woodcutting.listener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.framework.CoreNamespaceKeys;
import me.mykindos.betterpvp.core.framework.blocktag.BlockTaggingListener;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.progression.Progression;
import me.mykindos.betterpvp.progression.profession.woodcutting.event.PlayerStripLogEvent;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * This class's purpose is to remove the player-placed data on a log when it is stripped
 */
@BPvPListener
@Singleton
public class StrippedLogListener implements Listener {

    /**
     * This is the primary listener of the class.
     * Its purpose is to remove the player-placed data on a block when it is stripped
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerStripsLog(PlayerStripLogEvent event) {
        if (event.wasEventDeniedAndCancelled()) return;

        // If the initial log was placed by a player, then keep the player placed key
        Block block = event.getStrippedLog();
        PersistentDataContainer regularLogPdc = UtilBlock.getPersistentDataContainer(block);
        if (regularLogPdc.has(CoreNamespaceKeys.PLAYER_PLACED_KEY)) return;

        String expectedStrippedLog = "STRIPPED_" + block.getType().name();

        UtilServer.runTaskLater(JavaPlugin.getPlugin(Progression.class), () -> {

            // This stops players from mining the log and putting a more profitable log there instead
            // (like a mangrove log)
            if (!block.getType().name().equalsIgnoreCase(expectedStrippedLog)) return;

            PersistentDataContainer strippedLogPdc = UtilBlock.getPersistentDataContainer(block);
            if (!strippedLogPdc.has(CoreNamespaceKeys.PLAYER_PLACED_KEY)) return;

            strippedLogPdc.remove(CoreNamespaceKeys.PLAYER_PLACED_KEY);
            UtilBlock.setPersistentDataContainer(block, strippedLogPdc);
        }, BlockTaggingListener.DELAY_FOR_PROCESS_BLOCK_TAGS + 2L);
        // this should fire two ticks later than block tag
    }

}
