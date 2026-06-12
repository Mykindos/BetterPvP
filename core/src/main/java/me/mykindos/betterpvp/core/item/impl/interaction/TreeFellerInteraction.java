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
import me.mykindos.betterpvp.core.interaction.condition.ConditionResult;
import me.mykindos.betterpvp.core.interaction.context.InputMeta;
import me.mykindos.betterpvp.core.interaction.context.InteractionContext;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.impl.interaction.event.TreeFellerEvent;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
 * Attached as a base (non-serialized) component on axes,
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
    private final ItemFactory itemFactory;

    private final Set<Location> blocksBeingFelled = new HashSet<>();

    public boolean isFelling(Block block) {
        return blocksBeingFelled.contains(block.getLocation());
    }

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
                                  CooldownManager cooldownManager, ItemFactory itemFactory) {
        super("Tree Feller", cooldownManager);
        this.blockTagManager = blockTagManager;
        this.effectManager = effectManager;
        this.itemFactory = itemFactory;

        // Suppress re-entry: when our own scheduled UtilBlock.breakBlock fires a BlockBreakEvent,
        // the broken block's location will be in blocksBeingFelled, so we silently no-op.
        addCondition((actor, context) -> {
            Block broken = context.getOrNull(InputMeta.BROKEN_BLOCK);
            if (broken != null && blocksBeingFelled.contains(broken.getLocation())) {
                return ConditionResult.fail();
            }
            return ConditionResult.success();
        });
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
        collectTree(block, new HashSet<>(), blocksToFell);

        UtilServer.callEvent(new TreeFellerEvent(player, initialLogLocation, initialLogType, blocksToFell));

        for (int i = 0; i < blocksToFell.size(); i++) {
            Block blockToFell = blocksToFell.get(i);
            Location fellLocation = blockToFell.getLocation();
            blocksBeingFelled.add(fellLocation);
            UtilServer.runTaskLater(JavaPlugin.getPlugin(Core.class), () -> {
                final ItemStack mainHand = player.getInventory().getItemInMainHand();
                if (itemInstance != null && !itemFactory.isItemOfType(mainHand, itemInstance.getBaseItem())) {
                    blocksBeingFelled.remove(fellLocation);
                    // Don't cancel the task to ensure blocksBeingFelled is cleaned up
                    return;
                }

                try {
                    if (isBreakableLog(blockToFell, initialLogLocation)) {
                        UtilBlock.breakBlock(player, blockToFell);
                    }
                } finally {
                    blocksBeingFelled.remove(fellLocation);
                }
            }, (long) (i * 0.3));
        }

        player.getWorld().playSound(player.getLocation(), Sound.ITEM_AXE_STRIP, 2.0f, 1.0f);
        UtilMessage.message(player, "core.prefix.woodcutting", "core.interaction.tree_feller.used",
                Translations.component("core.interaction.tree_feller.name").color(NamedTextColor.GREEN));

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
     */
    private void collectTree(@NotNull Block block, @NotNull Set<Location> visited, @NotNull List<Block> blocksToFell) {
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

                        if (isTraversable(target) && visited.add(target.getLocation())) {
                            queue.add(target);
                        }
                    }
                }
            }
        }

    }

    @Override
    public @NotNull Component getDisplayName() {
        return Translations.component("core.ability.tree-feller.name");
    }

    @Override
    public @NotNull Component getDisplayDescription() {
        return Translations.component("core.ability.tree-feller.description");
    }
}
