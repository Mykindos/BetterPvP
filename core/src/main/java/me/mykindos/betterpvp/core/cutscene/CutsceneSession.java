package me.mykindos.betterpvp.core.cutscene;

import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.cutscene.camera.Beat;
import me.mykindos.betterpvp.core.cutscene.camera.CameraDriver;
import me.mykindos.betterpvp.core.cutscene.camera.CameraMarker;
import me.mykindos.betterpvp.core.cutscene.camera.CameraPose;
import me.mykindos.betterpvp.core.cutscene.camera.Cinematic;
import me.mykindos.betterpvp.core.cutscene.camera.Easing;
import me.mykindos.betterpvp.core.cutscene.effect.ShotEffect;
import me.mykindos.betterpvp.core.cutscene.hud.CutsceneActionBar;
import me.mykindos.betterpvp.core.cutscene.signal.Signal;
import me.mykindos.betterpvp.core.cutscene.signal.SignalBus;
import me.mykindos.betterpvp.core.cutscene.skip.SkipPolicy;
import me.mykindos.betterpvp.core.cutscene.skip.SkipScope;
import me.mykindos.betterpvp.core.cutscene.skip.SkipVerdict;
import me.mykindos.betterpvp.core.utilities.model.display.DisplayObject;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * One player's run through one {@link Cutscene}: the camera marker, the signal bus both tracks meet on, the beat the
 * camera is currently holding, and the effects that belong to it.
 * <p>
 * The session drives itself a tick at a time and reports whether it is still going, so {@link CutsceneManager} owns
 * scheduling and nothing else. Poses are resolved once when the camera track is attached rather than looked up per
 * tick, which is also what turns a missing {@code camera} marker into a refusal to start rather than a cutscene that
 * breaks halfway through.
 */
@Getter
public class CutsceneSession {

    private final UUID viewerId;
    private final Cutscene definition;
    private final SignalBus signals = new SignalBus();
    private final CompletableFuture<Boolean> completion = new CompletableFuture<>();

    /** Whether this player has finished this cutscene before - resolved once, read by {@link SkipPolicy#seenBefore}. */
    private final boolean seenBefore;

    private @Nullable CameraMarker marker;
    private @Nullable Cinematic camera;

    /** Resolved world positions, index-aligned with the camera track's beats. */
    private List<CameraPose> poses = List.of();

    private final List<ShotEffect> activeEffects = new ArrayList<>();

    /** Ticks since the cutscene started. The clock every signal firing is stamped with. */
    private int tick;

    private int beatIndex = -1;
    private int beatStartTick;
    private @Nullable CameraDriver driver;
    private boolean arrivalAnnounced;

    @Setter
    private boolean finished;

    /** Consecutive ticks the viewer has held SNEAK, which is how a beat skip is told from a cutscene skip. */
    @Setter
    private int sneakTicks;

    /** The action-bar surface this cutscene has taken over, and the boss-bar overlay carrying the top letterbox bar. */
    @Setter
    private @Nullable CutsceneActionBar actionBar;

    @Setter
    private @Nullable DisplayObject<Component> overlay;

    public CutsceneSession(@NotNull UUID viewerId, @NotNull Cutscene definition, boolean seenBefore) {
        this.viewerId = viewerId;
        this.definition = definition;
        this.seenBefore = seenBefore;
    }

    public @Nullable Player getPlayer() {
        return Bukkit.getPlayer(viewerId);
    }

    public @NotNull String getCutsceneId() {
        return definition.getId();
    }

    public boolean hasCamera() {
        return camera != null && !camera.isEmpty() && marker != null;
    }

    /** Records a signal against the current tick, so anything waiting on it from now on sees it. */
    public void emit(@NotNull Signal signal) {
        signals.emit(signal, tick);
    }

    public @Nullable Beat currentBeat() {
        if (camera == null || beatIndex < 0 || beatIndex >= camera.getBeats().size()) {
            return null;
        }
        return camera.getBeats().get(beatIndex);
    }

    /**
     * Binds a resolved camera track to this session. Called when the cutscene starts with one, and again if a dialogue
     * response grows one part-way through - which is why it is separate from the constructor.
     */
    void attachCamera(@NotNull Cinematic cinematic, @NotNull List<CameraPose> resolvedPoses, @NotNull CameraMarker cameraMarker) {
        this.camera = cinematic;
        this.poses = List.copyOf(resolvedPoses);
        this.marker = cameraMarker;
        this.beatIndex = -1;
    }

    /**
     * Advances one tick.
     *
     * @return false once the camera track has run out of beats and the session should be torn down
     */
    boolean tick() {
        tick++;
        if (camera == null || marker == null) {
            // Dialogue-only: the session lives until the conversation ends and tells the manager so.
            return true;
        }

        final Beat beat = currentBeat();
        if (beat == null) {
            return false;
        }

        final int ticksInBeat = tick - beatStartTick;
        if (driver != null) {
            marker.apply(driver.poseAt(ticksInBeat));
        }

        if (driver == null || !driver.hasArrived(ticksInBeat)) {
            return true;
        }

        // Arrival is announced once, on the first tick the camera is in place, so a dialogue node waiting on this beat
        // starts speaking exactly when the shot it belongs to is on screen.
        if (!arrivalAnnounced) {
            arrivalAnnounced = true;
            emit(Signal.beat(beat.getId()));
        }

        final int ticksSinceArrival = ticksInBeat - driver.getTravelTicks();
        if (beat.getAdvance().isSatisfied(this, ticksSinceArrival)) {
            return advance();
        }
        return true;
    }

    /**
     * Leaves the current beat for the next one.
     *
     * @return false if there was no next beat and the cutscene is over
     */
    boolean advance() {
        if (camera == null || marker == null) {
            return false;
        }

        final Beat leaving = currentBeat();
        if (leaving != null) {
            exitEffects();
            emit(Signal.beatEnd(leaving.getId()));
        }

        final int next = beatIndex + 1;
        if (next >= camera.getBeats().size()) {
            return false;
        }
        enterBeat(next);
        return true;
    }

    /** Jumps straight to a beat, leaving the current one properly. The entry point for a mid-timeline playback. */
    void enterBeat(int index) {
        if (camera == null || marker == null || index < 0 || index >= camera.getBeats().size()) {
            return;
        }

        final Beat beat = camera.getBeats().get(index);
        final CameraPose target = poses.get(index);
        final CameraPose from = beatIndex < 0 ? target : currentPose();

        beatIndex = index;
        beatStartTick = tick;
        arrivalAnnounced = false;
        driver = new CameraDriver(from, target, beat.getTravelTicks(), beat.getEasing());

        if (beat.getTravelTicks() == 0 || beat.getEasing() == Easing.CUT) {
            marker.snap(target);
        }

        for (ShotEffect effect : beat.getEffects()) {
            activeEffects.add(effect);
            effect.enter(this);
        }
    }

    /** Where the camera is right now, which is where the next travel starts from. */
    private @NotNull CameraPose currentPose() {
        if (driver == null) {
            return poses.get(Math.max(0, beatIndex));
        }
        return driver.poseAt(tick - beatStartTick);
    }

    /** Runs every active effect's exit. Called on a beat change, a skip, and an abort alike. */
    void exitEffects() {
        for (ShotEffect effect : activeEffects) {
            effect.exit(this);
        }
        activeEffects.clear();
    }

    /** The policy governing right now: the current beat's override, else the cutscene's own. */
    public @NotNull SkipPolicy skipPolicy() {
        final Beat beat = currentBeat();
        if (beat != null && beat.getSkip() != null) {
            return beat.getSkip();
        }
        return definition.getSkip();
    }

    public @NotNull SkipVerdict skipVerdict(@NotNull SkipScope scope) {
        return skipPolicy().evaluate(this, scope);
    }
}
