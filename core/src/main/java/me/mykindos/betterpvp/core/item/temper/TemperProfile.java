package me.mykindos.betterpvp.core.item.temper;

import com.google.common.base.Preconditions;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.item.config.Config;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

/**
 * Tuning for an item's temper: how quickly using its ability spends the bar, how quickly the bar
 * comes back, and what has to be true of the wielder for it to come back at all.
 * <p>
 * Durations are in seconds because that is the unit a designer tunes in; the per-second fractions
 * the service works in are derived. A profile belongs to the {@link me.mykindos.betterpvp.core.item.BaseItem}
 * and is shared by reference with every instance of that item, so retuning it on a config reload
 * applies to swords that already exist in the world.
 */
@Getter
@Setter
public class TemperProfile {

    /** Seconds of continuous use it takes to spend a full bar. */
    private double useDuration;
    /** Seconds it takes to refill an empty bar once recovery has started. */
    private double recoveryDuration;
    /** Seconds after the last drain before recovery starts. */
    private double recoveryDelay;
    /** Temper (0-1) the bar must hold before the ability may be started again. */
    private double minimumToUse;

    /**
     * An extra requirement on the wielder for recovery to progress, such as being on the ground.
     * Null means temper always recovers, which additionally lets it recover while stowed.
     */
    private @Nullable Predicate<LivingEntity> recoveryCondition;

    public TemperProfile(double useDuration, double recoveryDuration, double recoveryDelay) {
        setUseDuration(useDuration);
        setRecoveryDuration(recoveryDuration);
        this.recoveryDelay = recoveryDelay;
    }

    public void setUseDuration(double useDuration) {
        Preconditions.checkArgument(useDuration > 0, "Use duration must be greater than 0");
        this.useDuration = useDuration;
    }

    public void setRecoveryDuration(double recoveryDuration) {
        Preconditions.checkArgument(recoveryDuration > 0, "Recovery duration must be greater than 0");
        this.recoveryDuration = recoveryDuration;
    }

    /** Fraction of a full bar spent per second of continuous use. */
    public double drainPerSecond() {
        return 1d / useDuration;
    }

    /** Fraction of a full bar recovered per second. */
    public double recoveryPerSecond() {
        return 1d / recoveryDuration;
    }

    /**
     * Whether temper keeps recovering while the item sits in an inventory. A condition on the
     * wielder can only be evaluated while they are actually holding the item, so a conditional
     * profile necessarily freezes when stowed.
     */
    public boolean recoversWhileStowed() {
        return recoveryCondition == null;
    }

    public boolean canRecover(@Nullable LivingEntity wielder) {
        return recoveryCondition == null || (wielder != null && recoveryCondition.test(wielder));
    }

    /**
     * Overlays the {@code temper.*} section of an item's config onto this profile, keeping the
     * values it was constructed with as the defaults. The recovery condition stays in code, since
     * it is behaviour rather than tuning.
     */
    public void configure(@NotNull Config config) {
        final Config temper = config.fork("temper");
        setUseDuration(temper.getConfig("useDuration", useDuration, Double.class));
        setRecoveryDuration(temper.getConfig("recoveryDuration", recoveryDuration, Double.class));
        setRecoveryDelay(temper.getConfig("recoveryDelay", recoveryDelay, Double.class));
        setMinimumToUse(temper.getConfig("minimumToUse", minimumToUse, Double.class));
    }

    /**
     * A wielder must have both feet on the ground for temper to come back.
     */
    public TemperProfile recoveringWhileGrounded() {
        this.recoveryCondition = UtilBlock::isGrounded;
        return this;
    }

    /**
     * The bar must be full again before the ability may be restarted, rather than merely non-empty.
     */
    public TemperProfile requiringFullBar() {
        this.minimumToUse = 1;
        return this;
    }

    /** Empties after 12 seconds of use and takes 10 to come back. */
    public static TemperProfile slow() {
        return new TemperProfile(12, 10, 2);
    }

    /** Empties after 8 seconds of use and takes 9 to come back. */
    public static TemperProfile medium() {
        return new TemperProfile(8, 9, 2);
    }

    /** Empties after 5 seconds of use and takes 8 to come back. */
    public static TemperProfile mediumFast() {
        return new TemperProfile(5, 8, 2);
    }

    /** Empties after 3 seconds of use and takes 7 to come back. */
    public static TemperProfile fast() {
        return new TemperProfile(3, 7, 2);
    }
}
