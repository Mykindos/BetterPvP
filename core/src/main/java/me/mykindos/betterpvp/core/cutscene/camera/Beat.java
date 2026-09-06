package me.mykindos.betterpvp.core.cutscene.camera;

import lombok.Getter;
import me.mykindos.betterpvp.core.cutscene.effect.ShotEffect;
import me.mykindos.betterpvp.core.cutscene.signal.Signal;
import me.mykindos.betterpvp.core.cutscene.skip.SkipPolicy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * One shot: where the camera goes, how it gets there, when it leaves, and what is true while it is there.
 * <p>
 * The four are independent on purpose. Re-timing a shot does not touch what ends it; changing what ends it does not
 * touch what it shows. A beat carries no absolute position in the timeline, which is what lets one be dropped into the
 * middle of a cutscene without renumbering anything after it.
 */
@Getter
public final class Beat {

    /** The {@code camera} marker's {@code id:} tag, and the name this beat's signals are derived from. */
    private final String id;

    /** How long the camera takes to travel here from the previous beat. {@code 0} arrives instantly. */
    private final int travelTicks;

    private final Easing easing;
    private final BeatAdvance advance;
    private final List<ShotEffect> effects;

    /** Overrides the cutscene's policy for this beat alone; {@code null} inherits it. */
    private final @Nullable SkipPolicy skip;

    /**
     * The signal this beat waits on, when it waits on one, recorded alongside the opaque {@link #advance}.
     * <p>
     * The advance is a lambda and nothing can see inside it, so a validator could otherwise never tell a beat waiting
     * for a node that exists from one waiting for a node somebody renamed - which is precisely the mistake that hangs
     * a cutscene forever. Declaring it costs one field and turns that class of bug into a boot-time warning.
     */
    private final @Nullable Signal awaits;

    Beat(@NotNull String id, int travelTicks, @NotNull Easing easing, @NotNull BeatAdvance advance,
         @NotNull List<ShotEffect> effects, @Nullable SkipPolicy skip, @Nullable Signal awaits) {
        this.id = id;
        this.travelTicks = travelTicks;
        this.easing = easing;
        this.advance = advance;
        this.effects = List.copyOf(effects);
        this.skip = skip;
        this.awaits = awaits;
    }
}
