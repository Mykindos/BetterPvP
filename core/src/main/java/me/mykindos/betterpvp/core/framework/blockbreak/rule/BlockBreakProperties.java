package me.mykindos.betterpvp.core.framework.blockbreak.rule;

import com.google.common.base.Preconditions;
import lombok.Getter;
import lombok.ToString;

/**
 * Immutable value: how a block behaves when broken under a specific rule.
 * <p>
 * Break speed is in the framework's scaled units (vanilla tool speed × 15) and
 * is expressed as a non-negative {@code int}. Must always be strictly positive —
 * a value of {@code 1} is the slowest allowed. To express "cannot break", set
 * {@link #breakable} to {@code false}; the speed field is then meaningless but
 * still kept positive for invariant simplicity.
 */
@Getter
@ToString
public final class BlockBreakProperties {

    public static final int MIN_SPEED = 1;

    private final boolean breakable;
    private final int breakSpeed;

    public BlockBreakProperties(boolean breakable, int breakSpeed) {
        Preconditions.checkArgument(breakSpeed > 0, "breakSpeed must be > 0 (got %s)", breakSpeed);
        this.breakable = breakable;
        this.breakSpeed = breakSpeed;
    }

    public static BlockBreakProperties unbreakable() {
        return new BlockBreakProperties(false, MIN_SPEED);
    }

    public static BlockBreakProperties breakable(int speed) {
        return new BlockBreakProperties(true, speed);
    }

    /**
     * Additive merge of two rule outcomes (tool + global).
     * Unbreakable always wins: if either side is unbreakable, the merged result is unbreakable.
     * Otherwise speeds add. Saturating add to avoid overflow on absurd stacking.
     */
    public BlockBreakProperties merge(BlockBreakProperties other) {
        if (other == null) return this;
        if (!this.breakable || !other.breakable) return unbreakable();
        final long sum = (long) this.breakSpeed + (long) other.breakSpeed;
        final int capped = sum > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) sum;
        return new BlockBreakProperties(true, capped);
    }
}
