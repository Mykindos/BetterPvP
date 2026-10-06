package me.mykindos.betterpvp.core.block.custom;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.block.SmartBlock;
import me.mykindos.betterpvp.core.block.SmartBlockInstance;
import me.mykindos.betterpvp.core.block.SmartBlockInteractEvent;
import me.mykindos.betterpvp.core.block.SmartBlockInteractionService;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.NotePlayEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Optional;

/**
 * Player and world interaction with pack-drawn smart blocks: clicks reach the block's own handling, their items place
 * them, and custom note blocks never change state, play or move.
 */
@Singleton
public class PackSmartBlockInteractionService implements SmartBlockInteractionService, Listener {

    private final PackSmartBlockFactory factory;
    private final NoteBlockStates states;
    private final ItemFactory itemFactory;

    @Inject
    public PackSmartBlockInteractionService(Core core, PackSmartBlockFactory factory, NoteBlockStates states, ItemFactory itemFactory) {
        this.factory = factory;
        this.states = states;
        this.itemFactory = itemFactory;
        Bukkit.getPluginManager().registerEvents(this, core);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || event.getClickedBlock() == null) {
            return;
        }
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.LEFT_CLICK_BLOCK) {
            return;
        }
        final Player player = event.getPlayer();
        final Block clicked = event.getClickedBlock();
        final Optional<SmartBlockInstance> instance = factory.from(clicked);
        if (instance.isPresent()) {
            final SmartBlockInstance block = instance.get();
            if (!new SmartBlockInteractEvent(player, block, event.getAction()).callEvent()
                    || block.getType().handleClick(block, player, event.getAction())) {
                event.setCancelled(true);
                return;
            }
            if (block.getType() instanceof FurnitureBlock) {
                return;
            }
            // A custom note block never cycles its note, but blocks can still be placed against it.
            event.setUseInteractedBlock(Event.Result.DENY);
        }
        tryPlace(event, player, clicked);
    }

    private void tryPlace(PlayerInteractEvent event, Player player, Block clicked) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getBlockFace() == null) {
            return;
        }
        final ItemStack held = player.getInventory().getItemInMainHand();
        final Optional<ItemInstance> item = itemFactory.fromItemStack(held);
        if (item.isEmpty() || !(item.get().getBaseItem() instanceof PlacesBlock places)) {
            return;
        }
        final Optional<SmartBlock> type = factory.byBlockId(places.getBlockId());
        if (type.isEmpty()) {
            return;
        }
        event.setCancelled(true);
        final Block target = PackSmartBlockFactory.placementTarget(clicked, event.getBlockFace());
        if (factory.place(player, type.get(), target, held, clicked).isPresent()) {
            player.swingMainHand();
            if (player.getGameMode() != GameMode.CREATIVE) {
                held.subtract();
            }
        }
    }

    /**
     * A vanilla break of a pack-drawn block, such as a staff member in creative, takes the whole block with it and
     * drops its item instead of a note block.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        final Block block = event.getBlock();
        if (factory.isBreaking(block) || block.getType() != Material.NOTE_BLOCK && block.getType() != Material.BARRIER) {
            return;
        }
        factory.from(block).ifPresent(instance -> {
            event.setCancelled(true);
            factory.breakBlock(event.getPlayer(), instance);
        });
    }

    @EventHandler(ignoreCancelled = true)
    public void onNotePlay(NotePlayEvent event) {
        if (isCustom(event.getBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPhysics(BlockPhysicsEvent event) {
        if (event.getBlock().getType() == Material.NOTE_BLOCK && isCustom(event.getBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        if (anyCustom(event.getBlocks())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        if (anyCustom(event.getBlocks())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        event.blockList().removeIf(this::isCustom);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        event.blockList().removeIf(this::isCustom);
    }

    private boolean anyCustom(List<Block> blocks) {
        return blocks.stream().anyMatch(this::isCustom);
    }

    private boolean isCustom(Block block) {
        return states.blockOf(block.getBlockData()).isPresent();
    }

}
