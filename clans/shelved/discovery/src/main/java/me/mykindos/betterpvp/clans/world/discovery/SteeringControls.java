package me.mykindos.betterpvp.clans.world.discovery;

import lombok.Getter;
import me.mykindos.betterpvp.core.framework.shader.ScreenEffectService;
import org.jetbrains.annotations.NotNull;

/**
 * The pair of controls flanking one expedition's helm, and the crew's hands on them.
 * <p>
 * The physical props live here; who is holding them lives in {@link SteeringInput}, which knows nothing about the
 * world — so the question the ship actually asks each tick ("what is the net input?") is answerable on its own.
 */
@Getter
public class SteeringControls {

    private final SteerControl port;
    private final SteerControl starboard;
    private final SteeringInput input;
    private final SteeringCues cues;

    /** Which helm these are bolted to, as a {@link me.mykindos.betterpvp.clans.world.ship.BerthKey}. */
    private final String helmKey;

    public SteeringControls(@NotNull SteerControl port, @NotNull SteerControl starboard, @NotNull SteeringInput input,
                            @NotNull String helmKey, @NotNull ScreenEffectService screenEffects) {
        this.port = port;
        this.starboard = starboard;
        this.input = input;
        this.helmKey = helmKey;
        this.cues = new SteeringCues(screenEffects);
    }

    public @NotNull SteerControl control(@NotNull SteerSide side) {
        return side == SteerSide.PORT ? port : starboard;
    }

    public int netInput(long now) {
        return input.net(now);
    }

    /** Takes both controls out of the world for good. */
    public void remove() {
        port.remove();
        starboard.remove();
    }
}
