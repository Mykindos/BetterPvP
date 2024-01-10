package me.mykindos.betterpvp.champions.champions.skills.data;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

/**
 * Represents the charge data for a player using a charge skill
 * The charge can be between 0 and 1.
 * 0 being no charge and 1 being fully charged
 */
@Data
@RequiredArgsConstructor
public class ChargeData {

    private float charge = 0; // 0 -> 1
    private final float chargePerSecond; // 0 -> 1
    private long lastSound = 0;
    private long lastMessage = 0;
    private long soundInterval = 150; // In millis

    /**
     * Gain charge for this skill.
     * This method assumes that the call is being made every tick (20 times a second)
     */
    public void tick() {
        // Divide over 100 to get multiplication factor since it's in 100% scale for display
        this.charge = Math.min(1, this.charge + (chargePerSecond / 20));
    }

    public void tickSound(Player player) {
        if (!UtilTime.elapsed(lastSound, soundInterval)) {
            return;
        }

        player.playSound(player.getEyeLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1f + (0.5f * charge));
        lastSound = System.currentTimeMillis();
    }
}