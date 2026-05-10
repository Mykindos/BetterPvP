package me.mykindos.betterpvp.core.block;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.Predicate;

/**
 * Composite of break-related overrides a {@link SmartBlock} can supply to the
 * block-break framework. All fields are optional — a missing value means
 * "delegate to the underlying block's defaults" (Nexo {@code Breakable} or
 * vanilla, depending on what's present).
 *
 * <p>Field semantics:
 * <ul>
 *   <li>{@link #hardness()} — vanilla scale (stone=1.5, obsidian=50). Same units as
 *       {@code Material#getHardness()}.</li>
 *   <li>{@link #requiredTool()} — predicate that gates whether
 *       {@link #toolSpeedMultiplier()} applies. Empty predicate means the multiplier
 *       always applies. Never makes a block unbreakable on its own — that's
 *       {@link #unbreakable()}'s job.</li>
 *   <li>{@link #toolSpeedMultiplier()} — multiplier applied to break speed when
 *       {@link #requiredTool()} matches the held item (or always, if empty).</li>
 *   <li>{@link #speedMultiplier()} — multiplier applied unconditionally.</li>
 *   <li>{@link #unbreakable()} — short-circuits resolution to
 *       {@code BlockBreakProperties.unbreakable()}.</li>
 * </ul>
 */
public record SmartBlockBreakOverride(
        @NotNull OptionalDouble hardness,
        @NotNull Optional<Predicate<ItemStack>> requiredTool,
        @NotNull OptionalDouble toolSpeedMultiplier,
        @NotNull OptionalDouble speedMultiplier,
        boolean unbreakable
) {

    private static final SmartBlockBreakOverride EMPTY = new SmartBlockBreakOverride(
            OptionalDouble.empty(), Optional.empty(),
            OptionalDouble.empty(), OptionalDouble.empty(), false);

    public static @NotNull SmartBlockBreakOverride empty() {
        return EMPTY;
    }

    public static @NotNull Builder builder() {
        return new Builder();
    }

    /**
     * Per-field merge: this override's set fields take precedence; any field absent
     * here is filled in from {@code fallback}. {@code unbreakable} ORs (either side
     * forcing unbreakable wins).
     */
    public @NotNull SmartBlockBreakOverride merge(@NotNull SmartBlockBreakOverride fallback) {
        return new SmartBlockBreakOverride(
                hardness.isPresent() ? hardness : fallback.hardness,
                requiredTool.isPresent() ? requiredTool : fallback.requiredTool,
                toolSpeedMultiplier.isPresent() ? toolSpeedMultiplier : fallback.toolSpeedMultiplier,
                speedMultiplier.isPresent() ? speedMultiplier : fallback.speedMultiplier,
                unbreakable || fallback.unbreakable
        );
    }

    public @NotNull Builder toBuilder() {
        Builder b = new Builder();
        b.hardness = hardness;
        b.requiredTool = requiredTool;
        b.toolSpeedMultiplier = toolSpeedMultiplier;
        b.speedMultiplier = speedMultiplier;
        b.unbreakable = unbreakable;
        return b;
    }

    public static final class Builder {
        private OptionalDouble hardness = OptionalDouble.empty();
        private Optional<Predicate<ItemStack>> requiredTool = Optional.empty();
        private OptionalDouble toolSpeedMultiplier = OptionalDouble.empty();
        private OptionalDouble speedMultiplier = OptionalDouble.empty();
        private boolean unbreakable = false;

        public @NotNull Builder hardness(double value) {
            this.hardness = OptionalDouble.of(value);
            return this;
        }

        public @NotNull Builder requiredTool(@NotNull Predicate<ItemStack> predicate) {
            this.requiredTool = Optional.of(predicate);
            return this;
        }

        public @NotNull Builder toolSpeedMultiplier(double value) {
            this.toolSpeedMultiplier = OptionalDouble.of(value);
            return this;
        }

        public @NotNull Builder speedMultiplier(double value) {
            this.speedMultiplier = OptionalDouble.of(value);
            return this;
        }

        public @NotNull Builder unbreakable(boolean value) {
            this.unbreakable = value;
            return this;
        }

        public @NotNull SmartBlockBreakOverride build() {
            return new SmartBlockBreakOverride(
                    hardness, requiredTool, toolSpeedMultiplier, speedMultiplier, unbreakable);
        }
    }
}
