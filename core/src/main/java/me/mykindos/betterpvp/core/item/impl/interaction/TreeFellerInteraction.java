package me.mykindos.betterpvp.core.item.impl.interaction;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.properties.ClientProperty;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.framework.blocktag.BlockTagManager;
import me.mykindos.betterpvp.core.interaction.CooldownInteraction;
import me.mykindos.betterpvp.core.interaction.InteractionResult;
import me.mykindos.betterpvp.core.interaction.actor.InteractionActor;
import me.mykindos.betterpvp.core.interaction.context.InputMeta;
import me.mykindos.betterpvp.core.interaction.context.InteractionContext;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.impl.interaction.event.TreeFellerCompletedEvent;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Interaction that fells an entire tree when the player breaks a single log.
 * Attached as a base (non-serialized) component on {@link me.mykindos.betterpvp.core.item.impl.Axe},
 * so every axe type inherits tree-felling with zero per-subclass boilerplate.
 *
 * <p>Fires {@link TreeFellerCompletedEvent} after the tree is felled so that progression
 * perks (e.g. EnchantedLumberfall) can react without depending on this module.
 */
@Singleton
@CustomLog
public class TreeFellerInteraction extends CooldownInteraction {

    private final BlockTagManager blockTagManager;
    private final EffectManager effectManager;

    @Inject
    @Config(path = "items.treeFeller.cooldown", defaultValue = "20.0")
    private double cooldown;

    @Inject
    @Config(path = "items.treeFeller.maxBlocks", defaultValue = "15")
    private int maxBlocks;

    /**
     * Tracks how many blocks each player has felled in the current tree-fell call.
     * Reset to 0 before each execution and removed after.
     */
    private final Map<UUID, Integer> blocksFelledByPlayer = new HashMap<>();

    @Inject
    public TreeFellerInteraction(BlockTagManager blockTagManager, EffectManager effectManager,
                                  CooldownManager cooldownManager) {
        super("Tree Feller", cooldownManager);
        this.blockTagManager = blockTagManager;
        this.effectManager = effectManager;
    }

    @Override
    public double getCooldown() {
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

        // Respect the player's disable-tree-feller toggle
        Client client = actor.getClient();
        if (client != null) {
            boolean disabled = (boolean) client.getProperty(ClientProperty.DISABLE_TREEFELLER).orElse(false);
            if (disabled) {
                return new InteractionResult.Fail(InteractionResult.FailReason.CUSTOM);
            }
        }

        // Cancel the original break event; we handle all block breaking manually
        BlockBreakEvent breakEvent = context.getOrNull(InputMeta.BLOCK_BREAK_EVENT);
        if (breakEvent != null) {
            breakEvent.setCancelled(true);
        }

        Player player = (Player) actor.getEntity();
        UUID playerId = player.getUniqueId();

        // Save type before breaking (block becomes AIR after breakBlockNaturally)
        Material initialLogType = block.getType();
        Location initialLogLocation = block.getLocation();

        blocksFelledByPlayer.put(playerId, 0);
        Location firstLeafLocation = fellTree(player, block);
        blocksFelledByPlayer.remove(playerId);

        player.getWorld().playSound(player.getLocation(), Sound.ITEM_AXE_STRIP, 2.0f, 1.0f);
        UtilMessage.simpleMessage(player, "Woodcutting", "You used <alt>Tree Feller</alt>");

        UtilServer.callEvent(new TreeFellerCompletedEvent(player, firstLeafLocation, initialLogLocation, initialLogType));

        return InteractionResult.Success.ADVANCE;
    }

    /**
     * Applies the cooldown with an expire-sound callback so the player hears
     * when Tree Feller is ready to use again.
     */
    @Override
    public void then(@NotNull InteractionActor actor, @NotNull InteractionContext context,
                     @NotNull InteractionResult result, @Nullable ItemInstance itemInstance,
                     @Nullable ItemStack itemStack) {
        if (result.isSuccess() && actor.isPlayer()) {
            Player player = (Player) actor.getEntity();
            cooldownManager.use(player,
                    getCooldownName(),
                    getCooldown(),
                    false,
                    true,
                    false,
                    null,
                    0,
                    cd -> player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 0.3f, 1.5f));
        }
    }

    /**
     * Recursively breaks all connected log blocks within {@code maxBlocks} of the initial chop.
     *
     * @param player the player chopping
     * @param block  the current log block to break and search from
     * @return the location of the first natural leaf block encountered (for EnchantedLumberfall),
     *         or null if none was found
     */
    @Nullable
    private Location fellTree(@NotNull Player player, @NotNull Block block) {
        UUID playerId = player.getUniqueId();
        int felled = blocksFelledByPlayer.getOrDefault(playerId, 0);
        if (felled >= maxBlocks) return null;
        if (blockTagManager.isPlayerPlaced(block)) return null;

        UtilBlock.breakBlockNaturally(block, player, effectManager);
        blocksFelledByPlayer.put(playerId, felled + 1);

        Location firstLeafLocation = null;

        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                for (int y = 0; y <= 1; y++) {
                    Block target = block.getRelative(x, y, z);
                    String typeName = target.getType().name();

                    if (firstLeafLocation == null && typeName.contains("LEAVES")
                            && !blockTagManager.isPlayerPlaced(target)) {
                        firstLeafLocation = target.getLocation();
                    }

                    if (typeName.contains("_LOG")) {
                        Location childLeaf = fellTree(player, target);
                        if (firstLeafLocation == null) {
                            firstLeafLocation = childLeaf;
                        }
                    }
                }
            }
        }

        return firstLeafLocation;
    }
}
