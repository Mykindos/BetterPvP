package me.mykindos.betterpvp.core.cutscene.camera;

import me.mykindos.betterpvp.core.cutscene.effect.ShotEffect;
import me.mykindos.betterpvp.core.cutscene.signal.Signal;
import me.mykindos.betterpvp.core.cutscene.skip.SkipPolicy;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * The camera track of a cutscene: an ordered list of {@link Beat}s.
 * <p>
 * Beats name a {@code camera} data-point by its {@code id:} tag rather than carrying coordinates, so moving a shot is
 * dragging a marker in the map editor and inserting one is a marker plus a line here. Nothing in a cinematic is
 * addressed by index, so nothing renumbers.
 *
 * <pre>{@code
 * Cinematic.builder()
 *         .shot("gate",      s -> s.cut().until(response("greet", "show_me")))
 *         .shot("mine",      s -> s.travel(2.5, Easing.EASE_IN_OUT).until(nodeEnd("mine_intro")))
 *         .shot("mine_face", s -> s.travel(1.2, Easing.EASE_OUT).hold(Duration.ofSeconds(2)))
 *         .build();
 * }</pre>
 */
public final class Cinematic {

    private final List<Beat> beats;

    private Cinematic(@NotNull List<Beat> beats) {
        this.beats = Collections.unmodifiableList(beats);
    }

    public static @NotNull Builder builder() {
        return new Builder();
    }

    public @NotNull List<Beat> getBeats() {
        return beats;
    }

    public boolean isEmpty() {
        return beats.isEmpty();
    }

    /** @return the index of the beat with this id, or {@code -1} - the entry point for {@code /cutscene from}. */
    public int indexOf(@NotNull String beatId) {
        for (int i = 0; i < beats.size(); i++) {
            if (beats.get(i).getId().equalsIgnoreCase(beatId)) {
                return i;
            }
        }
        return -1;
    }

    public @NotNull Optional<Beat> beat(@NotNull String beatId) {
        final int index = indexOf(beatId);
        return index < 0 ? Optional.empty() : Optional.of(beats.get(index));
    }

    /** Assembles the shots of one camera track, in the order they play. */
    public static final class Builder {

        private final List<Beat> beats = new ArrayList<>();

        private Builder() {
        }

        /**
         * Adds a shot.
         *
         * @param cameraId the {@code camera} marker's {@code id:} tag
         */
        public @NotNull Builder shot(@NotNull String cameraId, @NotNull Consumer<ShotBuilder> shot) {
            final ShotBuilder builder = new ShotBuilder(cameraId);
            shot.accept(builder);
            beats.add(builder.build());
            return this;
        }

        /** A shot that cuts straight in and holds for the default dwell. */
        public @NotNull Builder shot(@NotNull String cameraId) {
            return shot(cameraId, shot -> {
            });
        }

        public @NotNull Cinematic build() {
            return new Cinematic(new ArrayList<>(beats));
        }
    }

    /** One shot's four independent axes: where it travels from, how, when it leaves, and what it shows. */
    public static final class ShotBuilder {

        /** How long a shot dwells when its author does not say. Long enough to read as deliberate, short enough to notice. */
        private static final int DEFAULT_HOLD_TICKS = 60;

        private final String cameraId;
        private final List<ShotEffect> effects = new ArrayList<>();
        private int travelTicks;
        private Easing easing = Easing.CUT;
        private @Nullable BeatAdvance advance;
        private @Nullable SkipPolicy skip;
        private @Nullable Signal awaits;

        private ShotBuilder(@NotNull String cameraId) {
            this.cameraId = cameraId;
        }

        /** Arrive instantly, with no travel from the previous shot. The default, and what an opening shot wants. */
        public @NotNull ShotBuilder cut() {
            this.travelTicks = 0;
            this.easing = Easing.CUT;
            return this;
        }

        /** Travel here from the previous shot over {@code seconds}, distributed by {@code easing}. */
        public @NotNull ShotBuilder travel(double seconds, @NotNull Easing easing) {
            this.travelTicks = Math.max(0, (int) Math.round(seconds * 20));
            this.easing = this.travelTicks == 0 ? Easing.CUT : easing;
            return this;
        }

        /** @see #travel(double, Easing) */
        public @NotNull ShotBuilder travel(@NotNull Duration duration, @NotNull Easing easing) {
            return travel(duration.toMillis() / 1000.0, easing);
        }

        /** Travel here smoothly, easing in and out. */
        public @NotNull ShotBuilder travel(double seconds) {
            return travel(seconds, Easing.EASE_IN_OUT);
        }

        /** Dwell here for {@code duration} after arriving, then move on. */
        public @NotNull ShotBuilder hold(@NotNull Duration duration) {
            return until(BeatAdvance.hold((int) Math.max(0, Math.round(duration.toMillis() / 50.0))));
        }

        /** @see #hold(Duration) */
        public @NotNull ShotBuilder hold(double seconds) {
            return until(BeatAdvance.hold(Math.max(0, (int) Math.round(seconds * 20))));
        }

        /** Stay until {@code signal} fires - the rendezvous with the dialogue track. */
        public @NotNull ShotBuilder until(@NotNull Signal signal) {
            this.awaits = signal;
            return until(BeatAdvance.signal(signal));
        }

        /** Stay until {@code condition} holds for the viewer. */
        public @NotNull ShotBuilder until(@NotNull Predicate<Player> condition) {
            return until(BeatAdvance.when(condition));
        }

        public @NotNull ShotBuilder until(@NotNull BeatAdvance advance) {
            this.advance = advance;
            return this;
        }

        /** Things that are true for exactly as long as this shot is on screen. */
        public @NotNull ShotBuilder during(@NotNull ShotEffect @NotNull ... shotEffects) {
            Collections.addAll(effects, shotEffects);
            return this;
        }

        /** Overrides the cutscene's skip policy for this shot alone. */
        public @NotNull ShotBuilder skip(@NotNull SkipPolicy policy) {
            this.skip = policy;
            return this;
        }

        private @NotNull Beat build() {
            return new Beat(cameraId, travelTicks, easing,
                    advance == null ? BeatAdvance.hold(DEFAULT_HOLD_TICKS) : advance, effects, skip, awaits);
        }
    }
}
