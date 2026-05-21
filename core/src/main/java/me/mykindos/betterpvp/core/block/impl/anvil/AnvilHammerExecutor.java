package me.mykindos.betterpvp.core.block.impl.anvil;

import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import static me.mykindos.betterpvp.core.block.impl.anvil.AnvilConstants.SWING_COOLDOWN_MS;

/**
 * Tracks hammer-swing timing and the swing counter for an anvil.
 * <br>
 * This class is intentionally agnostic to <i>what</i> the swings accomplish — recipe
 * crafting and item repair both completion logic lives in
 * {@link me.mykindos.betterpvp.core.block.impl.anvil.operation.AnvilOperation}.
 */
@Getter
@Setter
public class AnvilHammerExecutor {

    private int hammerSwings = 0;
    private long lastSwingTime = 0L; // System.currentTimeMillis()

    /**
     * Checks if a player can swing the hammer (cooldown check).
     *
     * @return true if enough time has passed since the last swing
     */
    public boolean canSwing() {
        return System.currentTimeMillis() - lastSwingTime >= SWING_COOLDOWN_MS;
    }

    /**
     * Executes a hammer swing, incrementing the counter and playing effects.
     *
     * @param player   The player swinging the hammer
     * @param location The location to play effects at
     * @return true if the swing was executed (not on cooldown)
     */
    public boolean executeHammerSwing(@NotNull Player player, @NotNull Location location) {
        if (!canSwing()) {
            return false; // Cooldown not finished
        }

        lastSwingTime = System.currentTimeMillis();
        hammerSwings++;

        // Play hammer swing effects
        new SoundEffect(Sound.BLOCK_ANVIL_LAND, 0.7f, 0.4f).play(location);
        new SoundEffect(Sound.BLOCK_ANVIL_LAND, 1.2f + (float) Math.random() * 0.45f, 0.4f).play(location);
        player.swingMainHand();

        return true;
    }

    /**
     * Resets the hammer swing counter.
     */
    public void reset() {
        hammerSwings = 0;
    }
}
