package me.mykindos.betterpvp.core.block;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * Factory class for creating {@link SmartBlockInstance}s
 */
public interface SmartBlockFactory {

    /**
     * Creates a new {@link SmartBlockInstance} from the given location.
     * @param location the location to create the instance from
     * @return a new {@link SmartBlockInstance} if the block at the location is a smart block
     */
    Optional<SmartBlockInstance> from(Location location);

    /**
     * Creates a new {@link SmartBlockInstance} from the given block.
     * @param block the block to create the instance from
     * @return a new {@link SmartBlockInstance} if the block is a smart block
     */
    Optional<SmartBlockInstance> from(Block block);

    /**
     * Creates a new {@link SmartBlockInstance} from the player's target block.
     * @param player the player to create the instance from
     * @return a new {@link SmartBlockInstance} if the player's target block is a smart block, empty otherwise
     */
    Optional<SmartBlockInstance> fromTarget(Player player);

    /**
     * Loads a {@link SmartBlockInstance} from the given block for the first time.
     * This is only called when a chunk loads.
     * @param block the block to load the instance for
     * @return a new {@link SmartBlockInstance} if the block is a smart block, empty otherwise
     */
    Optional<SmartBlockInstance> load(Block block);

    /**
     * Checks if the given block is a smart block.
     * @param block  the block to check
     * @return true if the block is a smart block, false otherwise
     */
    boolean isSmartBlock(Block block);

    /**
     * Checks if the given location is a smart block.
     * @param location the location to check
     * @return true if the block at the location is a smart block, false otherwise
     */
    boolean isSmartBlock(Location location);

    /**
     * Checks if the given entity is a smart block.
     * @param entity the entity to check
     * @return true if the entity is a smart block, false otherwise
     */
    boolean isSmartBlock(Entity entity);

    /**
     * Checks if the player's target block is a smart block.
     * @param player the player to check
     * @return true if the player's target block is a smart block, false otherwise
     */
    boolean isTargetSmartBlock(@NotNull Player player);

    /**
     * Creates a new {@link BlockData} instance for the given smart block.
     * @param type the smart block to create the data for
     * @return a new {@link BlockData} instance
     */
    BlockData createBlockData(SmartBlock type);

    /**
     * Breaks the given block instance.
     * @param player the player who broke the block
     * @param instance the block instance to break
     * @return true if the block was broken, false otherwise
     */
    boolean breakBlock(Player player, SmartBlockInstance instance);

    /**
     * Implementation-supplied break-override defaults for this instance — typically
     * pulled from the underlying provider (e.g. Nexo's {@code Breakable}). Used as the
     * per-field fallback in {@link SmartBlockOverrides#resolve} when the
     * {@link SmartBlock}'s own override leaves a field absent.
     * <p>
     * Default implementation returns {@link SmartBlockBreakOverride#empty()}; concrete
     * provider-aware factories override.
     */
    default @NotNull SmartBlockBreakOverride getBreakOverrideDefaults(@NotNull SmartBlockInstance instance,
                                                                       @NotNull Player player,
                                                                       @NotNull ItemStack held) {
        return SmartBlockBreakOverride.empty();
    }

    /**
     * Render a per-tick break-progress indicator for a player who is mining {@code block}.
     * Provider-specific factories (Nexo, Oraxen) push a subtitle progress bar; vanilla
     * relies on the destruction-stage overlay from {@code WrapperPlayServerBlockBreakAnimation}
     * and implements this as a no-op.
     *
     * @param player   the breaking player
     * @param block    the block being broken
     * @param progress break progress in {@code [0, 1]}
     */
    void displayBreakProgress(@NotNull Player player, @NotNull Block block, double progress);
}
