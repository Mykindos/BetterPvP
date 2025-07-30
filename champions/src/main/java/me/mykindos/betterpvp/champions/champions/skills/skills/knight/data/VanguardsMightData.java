package me.mykindos.betterpvp.champions.champions.skills.skills.knight.data;

import lombok.Data;
import org.jetbrains.annotations.NotNull;

/**
 * Data class to hold the state of the Vanguards Might ability for each player.
 */
@Data
public class VanguardsMightData {

    /**
     * Charge is a value between 0 and 1, representing how much charge the player has accumulated. Cbarge is gained
     * through the player "absorbing" damage while channeling the skill.
     */
    private @NotNull Float charge;

    /**
     * The current phase of the ability. See {@link VanguardsMightAbilityPhase} for more details.
     */
    private @NotNull VanguardsMightAbilityPhase phase;

    /**
     * The charge accumulated during the transference phase, which is a value between 0 and 1.
     * This is purely cosmetic and acts as a tradeoff since this skill won't instantly give you strength compared to
     * Riposte which instantly gives you the boosted attack damage.
     * <p>
     * See transferencePhaseDuration for how long the transference phase lasts.
     */
    private Float transferenceCharge = 0f;

    /**
     * A flag to indicate whether the strength effect has already been applied or the action bar has been set.
     */
    private Boolean alreadyAppliedStrengthEffectOrActionBar = false;

    /**
     * The time left for the strength effect in seconds. This is used to update the action bar. This is purely
     * cosmetic and has no effect on the actual strength effect duration. Having this helps clean up the action bar
     * since cooldowns work a little weird with channel skills.
     */
    private Double strengthEffectTimeLeft = 0.0;
}
