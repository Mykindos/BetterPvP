package me.mykindos.betterpvp.core.item.impl.interaction;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.framework.blocktag.BlockTagManager;
import me.mykindos.betterpvp.core.interaction.CooldownInteraction;
import me.mykindos.betterpvp.core.interaction.DisplayedInteraction;
import me.mykindos.betterpvp.core.interaction.InteractionResult;
import me.mykindos.betterpvp.core.interaction.actor.InteractionActor;
import me.mykindos.betterpvp.core.interaction.actor.PlayerInteractionActor;
import me.mykindos.betterpvp.core.interaction.context.InputMeta;
import me.mykindos.betterpvp.core.interaction.context.InteractionContext;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.impl.interaction.event.TreeFellerEvent;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilItem;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;

/**
 * Interaction that fells an entire tree when the player breaks a single log.
 * Attached as a base (non-serialized) component on {@link me.mykindos.betterpvp.core.item.impl.Axe},
 * so every axe type inherits tree-felling with zero per-subclass boilerplate.
 *
 * <p>Fires {@link TreeFellerEvent} before the tree is felled so that progression
 * perks (e.g. EnchantedLumberfall) can react without depending on this module.
 */
@Singleton
@CustomLog
public class TreeFellerInteraction extends CooldownInteraction implements DisplayedInteraction {

    private final BlockTagManager blockTagManager;
    private final EffectManager effectManager;

    @Nullable
    private TreeFellerCooldownModifier cooldownModifier;

    @Inject
    @Config(path = "items.treeFeller.cooldown", defaultValue = "20.0")
    private double cooldown;

    @Inject
    @Config(path = "items.treeFeller.maxBlocks", defaultValue = "35")
    private int maxBlocks;

    @Inject
    public TreeFellerInteraction(BlockTagManager blockTagManager, EffectManager effectManager,
                                  CooldownManager cooldownManager) {
        super("Tree Feller", cooldownManager);
        this.blockTagManager = blockTagManager;
        this.effectManager = effectManager;
    }

    public void setModifier(@Nullable TreeFellerCooldownModifier modifier) {
        this.cooldownModifier = modifier;
    }

    @Override
    public double getCooldown(InteractionActor actor) {
        if (actor instanceof PlayerInteractionActor playerActor) {
            return Optional.ofNullable(cooldownModifier)
                    .map(m -> m.getEffectiveCooldown(playerActor.getPlayer(), cooldown))
                    .orElse(cooldown);
        }
        return cooldown;
    }

    @Override
    protected @NotNull InteractionResult doCooldownExecute(@NotNull InteractionActor actor,
                                                            @NotNull InteractionContext context,
                                                            @Nullable ItemInstance itemInstance,
                                                            @Nullable ItemStack itemStack) {
        Block block = context.getOrNull(InputMeta.BROKEN_BLOCK);
        if (block == null || !block.getType().name().contains("_LOG")) {
            return new InteractionResult.Fail(InteractionResult.FailReason.CUSTOM);
        }

        // Cancel the original break event; we handle all block breaking manually
        BlockBreakEvent breakEvent = context.getOrNull(InputMeta.BLOCK_BREAK_EVENT);
        if (breakEvent != null) {
            breakEvent.setCancelled(true);
        }

        Player player = (Player) actor.getEntity();

        // Save type before breaking (block becomes AIR after breakBlockNaturally)
        Material initialLogType = block.getType();
        Location initialLogLocation = block.getLocation();

        List<Block> blocksToFell = new ArrayList<>();
        Location firstLeafLocation = collectTree(block, new HashSet<>(), blocksToFell);

        UtilServer.callEvent(new TreeFellerEvent(player, firstLeafLocation, initialLogLocation, initialLogType, blocksToFell));

        final ItemStack itemInMainHand = player.getInventory().getItemInMainHand();
        for (int i = 0; i < blocksToFell.size(); i++) {
            Block blockToFell = blocksToFell.get(i);
            UtilServer.runTaskLater(JavaPlugin.getPlugin(Core.class), () -> {
                if (isBreakableLog(blockToFell, initialLogLocation)) {
                    UtilBlock.breakBlockNaturally(blockToFell, player, effectManager);
                    UtilItem.damageItem(player, itemInMainHand, 1);
                }
            }, (long) (i * 0.3));
        }

        player.getWorld().playSound(player.getLocation(), Sound.ITEM_AXE_STRIP, 2.0f, 1.0f);
        UtilMessage.simpleMessage(player, "Woodcutting", "You used <alt>Tree Feller</alt>");

        return InteractionResult.Success.ADVANCE;
    }

    private boolean isBreakableLog(@NotNull Block block, @NotNull Location initialLogLocation) {
        return isLog(block) && (!blockTagManager.isPlayerPlaced(block) || block.getLocation().equals(initialLogLocation));
    }

    private boolean isTraversable(@NotNull Block block) {
        return block.getType().name().contains("LEAVES") || isLog(block);
    }

    private boolean isLog(@NotNull Block block) {
        return block.getType().name().contains("_LOG");
    }

    /**
     * Collects breakable log blocks breadth-first, traversing through leaves and player-placed blocks.
     *
     * @param block  the current log block to collect and search from
     * @param visited blocks already searched during this tree-fell call
     * @param blocksToFell mutable list of blocks that will be exposed to listeners before breaking
     * @return the location of the first natural leaf block encountered (for EnchantedLumberfall),
     *         or null if none was found
     */
    @Nullable
    private Location collectTree(@NotNull Block block, @NotNull Set<Location> visited, @NotNull List<Block> blocksToFell) {
        Location firstLeafLocation = null;
        Queue<Block> queue = new ArrayDeque<>();
        queue.add(block);
        visited.add(block.getLocation());

        while (!queue.isEmpty() && blocksToFell.size() < maxBlocks) {
            Block current = queue.poll();

            if (isBreakableLog(current, block.getLocation())) {
                blocksToFell.add(current);
            }

            for (int y = 0; y <= 1; y++) {
                for (int x = -1; x <= 1; x++) {
                    for (int z = -1; z <= 1; z++) {
                        Block target = current.getRelative(x, y, z);
                        String typeName = target.getType().name();

                        if (firstLeafLocation == null && typeName.contains("LEAVES")
                                && !blockTagManager.isPlayerPlaced(target)) {
                            firstLeafLocation = target.getLocation();
                        }

                        if (isTraversable(target) && visited.add(target.getLocation())) {
                            queue.add(target);
                        }
                    }
                }
            }
        }

        return firstLeafLocation;
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.text("Tree Feller");
    }

    @Override
    public @NotNull Component getDisplayDescription() {
        return Component.text("Fells the entire tree when you break a log.");
    }
}
