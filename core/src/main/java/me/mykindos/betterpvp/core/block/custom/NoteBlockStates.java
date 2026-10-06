package me.mykindos.betterpvp.core.block.custom;

import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.resourcepack.PackReleaseChangedEvent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * The note block state that draws each custom block, from the current release.
 */
@CustomLog
@Singleton
@BPvPListener
public class NoteBlockStates implements Listener {

    private volatile Map<String, BlockData> byBlock = Map.of();
    private volatile Map<String, String> byState = Map.of();

    @EventHandler
    public void onReleaseChanged(PackReleaseChangedEvent event) {
        final Map<String, String> released = event.getCurrent().getBlocks();
        final Map<String, BlockData> blocks = new HashMap<>();
        final Map<String, String> states = new HashMap<>();
        if (released != null) {
            released.forEach((blockId, state) -> {
                try {
                    final BlockData data = Bukkit.createBlockData("minecraft:note_block[" + state + "]");
                    blocks.put(blockId, data);
                    states.put(data.getAsString(), blockId);
                } catch (IllegalArgumentException ex) {
                    log.error("Block {} has an invalid note block state {}", blockId, state, ex).submit();
                }
            });
        }
        byBlock = Map.copyOf(blocks);
        byState = Map.copyOf(states);
    }

    public Optional<BlockData> stateOf(String blockId) {
        return Optional.ofNullable(byBlock.get(blockId)).map(BlockData::clone);
    }

    /**
     * The custom block a note block state draws, if any.
     */
    public Optional<String> blockOf(BlockData data) {
        if (data.getMaterial() != Material.NOTE_BLOCK) {
            return Optional.empty();
        }
        return Optional.ofNullable(byState.get(data.getAsString()));
    }

}
