package me.mykindos.betterpvp.core.framework.blockbreak.rule;

import com.google.common.base.Preconditions;
import lombok.Getter;
import lombok.ToString;

/**
 * Immutable value: how a block behaves when broken under a specific rule.
 * <p>
 * Carries one of three payloads, picked by the rule's {@link RuleLayer}:
 * <ul>
 *   <li>An int {@code breakSpeed} for {@code ADDITIVE} / {@code OVERRIDE} rules
 *       and for tool base rules. In the framework's scaled units (vanilla × 15).</li>
 *   <li>A double {@code multiplier} for {@code MULTIPLICATIVE} rules. {@code 1.0} is
 *       a no-op; {@code 1.10} is +10%.</li>
 *   <li>{@link #unbreakable()} short-circuits the resolver regardless of layer.</li>
 * </ul>
 * The unused field for any given variant is left at a benign default
 * ({@code MIN_SPEED} / {@code 1.0}) so getters are always safe to call.
 */
@Getter
@ToString
public final class BlockBreakProperties {

    public static final int MIN_SPEED = 1;

    private final boolean breakable;
    private final int breakSpeed;
    private final double multiplier;

    private BlockBreakProperties(boolean breakable, int breakSpeed, double multiplier) {
        this.breakable = breakable;
        this.breakSpeed = breakSpeed;
        this.multiplier = multiplier;
    }

    public static BlockBreakProperties unbreakable() {
        return new BlockBreakProperties(false, MIN_SPEED, 1.0);
    }

    public static BlockBreakProperties breakable(int speed) {
        Preconditions.checkArgument(speed > 0, "breakSpeed must be > 0 (got %s)", speed);
        return new BlockBreakProperties(true, speed, 1.0);
    }

    public static BlockBreakProperties multiplier(double multiplier) {
        Preconditions.checkArgument(multiplier > 0.0, "multiplier must be > 0 (got %s)", multiplier);
        return new BlockBreakProperties(true, MIN_SPEED, multiplier);
    }
}
