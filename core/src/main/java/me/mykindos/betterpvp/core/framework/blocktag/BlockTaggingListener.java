package me.mykindos.betterpvp.core.framework.blocktag;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.framework.CoreNamespaceKeys;
import me.mykindos.betterpvp.core.framework.persistence.DataType;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.model.data.CustomDataType;
import org.bukkit.Chunk;
import org.bukkit.GameMode;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockFertilizeEvent;
import org.bukkit.event.block.BlockMultiPlaceEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.EntityBlockFormEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.block.SpongeAbsorbEvent;
import org.bukkit.event.block.TNTPrimeEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.WeakHashMap;

@BPvPListener
@Singleton
public class BlockTaggingListener implements Listener {

    private final Core core;

    private final ClientManager clientManager;

    private final WeakHashMap<Chunk, List<BlockTag>> blockTags = new WeakHashMap<>();

    @Inject
    public BlockTaggingListener(Core core, ClientManager clientManager) {
        this.core = core;
        this.clientManager = clientManager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        untagBlock(event.getBlock());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBurn(BlockBurnEvent event) {
        untagBlock(event.getBlock());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        event.blockList().forEach(this::untagBlock);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockFade(BlockFadeEvent event) {
        untagBlock(event.getBlock());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockFertilize(BlockFertilizeEvent event) {
        tagBlock(event.getBlock(), event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockMultiPlace(BlockMultiPlaceEvent event) {
        event.getReplacedBlockStates().forEach(blockState -> tagBlock(blockState.getBlock(), event.getPlayer()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        event.getBlocks().forEach(block -> shift(block, event.getDirection()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        if (!event.isSticky()) return;
        event.getBlocks().forEach(block -> shift(block, event.getDirection()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        tagBlock(event.getBlock(), event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityBlockForm(EntityBlockFormEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        tagBlock(event.getBlock(), player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLeafDecay(LeavesDecayEvent event) {
        untagBlock(event.getBlock());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPrime(TNTPrimeEvent event) {
        untagBlock(event.getBlock());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSpongeAbsorb(SpongeAbsorbEvent event) {
        event.getBlocks().forEach(state -> untagBlock(state.getBlock()));
    }

    private void shift(Block block, BlockFace direction) {
        final UUID player = getTaggedPlayer(block);
        if (player == null) return; // It wasn't a player placed block
        final Block relative = block.getRelative(direction);
        untagBlock(block);
        tagBlock(relative, player);
    }

    private UUID getTaggedPlayer(Block block) {
        final PersistentDataContainer pdc = UtilBlock.getPersistentDataContainer(block);
        return pdc.get(CoreNamespaceKeys.PLAYER_PLACED_KEY, CustomDataType.UUID);
    }

    private void tagBlock(Block block, Player player) {
        if (player == null || player.getGameMode() == GameMode.CREATIVE) return;
        if (clientManager.search().online(player).isAdministrating()) return;
        tagBlock(block, player.getUniqueId());
    }

    // Run next tick to allow other plugins to read the block
    private void tagBlock(Block block, UUID uuid) {
        List<BlockTag> tags = blockTags.computeIfAbsent(block.getChunk(), k -> new ArrayList<>());
        tags.add(new BlockTag(block, uuid, BlockTag.BlockTagType.TAG));
    }

    // Run next tick to allow other plugins to read the block
    private void untagBlock(Block block) {
        List<BlockTag> tags = blockTags.computeIfAbsent(block.getChunk(), k -> new ArrayList<>());
        tags.add(new BlockTag(block, null, BlockTag.BlockTagType.UNTAG));
    }

    @UpdateEvent
    public void processBlockTags() {
        if (blockTags.isEmpty()) return;

        UtilServer.runTaskLater(core, false, () -> {
            blockTags.forEach((chunk, tags) -> {
                final PersistentDataContainer chunkPdc = chunk.getPersistentDataContainer();
                if (!chunkPdc.has(CoreNamespaceKeys.BLOCK_TAG_CONTAINER_KEY, DataType.asHashMap(PersistentDataType.INTEGER, PersistentDataType.TAG_CONTAINER))) {
                    chunkPdc.set(CoreNamespaceKeys.BLOCK_TAG_CONTAINER_KEY, DataType.asHashMap(PersistentDataType.INTEGER, PersistentDataType.TAG_CONTAINER), new HashMap<>());
                }

                HashMap<Integer, PersistentDataContainer> blockContainers = UtilBlock.WEAK_BLOCKMAP_CACHE.get(chunk, key -> chunkPdc.get(CoreNamespaceKeys.BLOCK_TAG_CONTAINER_KEY, DataType.asHashMap(PersistentDataType.INTEGER, PersistentDataType.TAG_CONTAINER)));
                if (blockContainers != null) {
                    tags.forEach(blockTag -> {

                        int blockKey = UtilBlock.getBlockKey(blockTag.getBlock());
                        PersistentDataContainer blockPdc = blockContainers.get(blockKey);
                        if (blockPdc == null) {
                            blockContainers.put(blockKey, chunkPdc.getAdapterContext().newPersistentDataContainer());
                            blockPdc = blockContainers.get(blockKey);
                        }

                        if (blockTag.getTagType() == BlockTag.BlockTagType.TAG) {
                            if(blockTag.getTagger() != null) {
                                blockPdc.set(CoreNamespaceKeys.PLAYER_PLACED_KEY, CustomDataType.UUID, blockTag.getTagger());
                            }
                        } else {
                            blockPdc.remove(CoreNamespaceKeys.PLAYER_PLACED_KEY);
                        }

                        blockContainers.put(blockKey, blockPdc);

                    });

                }
            });

            blockTags.clear();
        }, 1L);


    }

}
