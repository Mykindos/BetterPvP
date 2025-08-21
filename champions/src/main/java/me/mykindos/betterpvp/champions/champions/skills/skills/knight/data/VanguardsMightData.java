package me.mykindos.betterpvp.champions.champions.skills.skills.knight.data;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.champions.champions.skills.data.ChargeData;
import org.jetbrains.annotations.NotNull;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class VanguardsMightData extends ChargeData {

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
    private float transferenceCharge = 0f;

    /**
     * A flag to indicate whether the strength effect has already been applied or the action bar has been set.
     */
    @Getter
    private boolean alreadyAppliedStrengthEffectOrActionBar = false;

    /**
     * The time left for the strength effect in seconds. This is used to update the action bar. This is purely
     * cosmetic and has no effect on the actual strength effect duration. Having this helps clean up the action bar
     * since cooldowns work a little weird with channel skills.
     */
    private double strengthEffectTimeLeft = 0.0;

    public VanguardsMightData(float chargePerSecond, @NotNull VanguardsMightAbilityPhase phase) {
        super(chargePerSecond);
        this.phase = phase;
    }

}
