package me.mykindos.betterpvp.champions.champions.skills.data;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import me.mykindos.betterpvp.champions.champions.skills.types.ChannelSkill;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.components.champions.ISkill;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import me.mykindos.betterpvp.core.utilities.model.ProgressBar;
import me.mykindos.betterpvp.core.utilities.model.display.DisplayComponent;
import me.mykindos.betterpvp.core.utilities.model.display.PermanentComponent;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

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
    private long soundInterval = 100; // In millis
    private long messageInterval = 250; // In millis

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

        if (charge < 1) {
            player.playSound(player.getEyeLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1f + charge);
            lastSound = System.currentTimeMillis();
        }
    }

    public void messageSent() {
        lastMessage = System.currentTimeMillis();
    }

    public boolean canSendMessage() {
        return UtilTime.elapsed(lastMessage, messageInterval);
    }

    /**
     * Get the action bar component for a {@link ChargeData}.
     * <br>
     * <b>Note: The result of this method should be saved to be reused instead of creating a new one for each player</b>
     *
     * @param showCondition Predicate to determine if the component should be shown
     * @param supplier      Supplier to get the {@link ChargeData} for a {@link Gamer}
     * @return The action bar component
     */
    public static DisplayComponent getActionBar(Predicate<Gamer> showCondition, Function<Gamer, ? extends ChargeData> supplier) {
        return new PermanentComponent(gamer -> {
            final Player player = gamer.getPlayer();
            if (player == null || !showCondition.test(gamer)) {
                return null; // Skip if not online or not showing
            }

            final ChargeData charge = supplier.apply(gamer);
            ProgressBar progressBar = ProgressBar.withProgress(charge.getCharge());
            return progressBar.build();
        });
    }

    /**
     * Get the action bar display for a {@link ChannelSkill}. Only displays if the player is holding skill item
     * and the player is in the charge map.
     *
     * @param skill         The skill to get the action bar for
     * @param chargeDataMap The map of players to charge data. This should be a reference to the map in the skill
     *                      and not a copy of it.
     * @see ChargeData#getActionBar(Predicate, Function)
     */
    public static DisplayComponent getActionBar(ISkill skill, Map<Player, ? extends ChargeData> chargeDataMap) {
        return getActionBar(
                gmr -> gmr.isOnline() && chargeDataMap.containsKey(gmr.getPlayer()) && skill.isHolding(gmr.getPlayer()),
                gmr -> chargeDataMap.get(gmr.getPlayer())
        );
    }

    /**
     * Get the action bar display for a {@link ChannelSkill}. Only displays if the player is holding skill item
     * and the player is in the charge map.
     *
     * @param skill                   The skill to get the action bar for
     * @param chargeDataMap           The map of players to charge data. This should be a reference to the map in the skill
     *                                and not a copy of it.
     * @param additionalShowCondition Additional condition to determine if the component should be shown. The
     *                                gamer is guaranteed to be online.
     * @see ChargeData#getActionBar(Predicate, Function)
     */
    public static DisplayComponent getActionBar(ISkill skill, Map<Player, ? extends ChargeData> chargeDataMap, Predicate<Gamer> additionalShowCondition) {
        return getActionBar(
                gmr -> gmr.isOnline() && chargeDataMap.containsKey(gmr.getPlayer()) && skill.isHolding(gmr.getPlayer()) && additionalShowCondition.test(gmr),
                gmr -> chargeDataMap.get(gmr.getPlayer())
        );
    }
}