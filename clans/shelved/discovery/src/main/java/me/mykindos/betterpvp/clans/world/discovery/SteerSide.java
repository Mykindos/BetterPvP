package me.mykindos.betterpvp.clans.world.discovery;

import lombok.Getter;

/**
 * Which of a helm's two flanking controls is meant.
 * <p>
 * The signs match {@link ShipDynamics}: a rising heading is a turn to starboard, so the starboard control asks for
 * {@code +1} and the port one for {@code -1}.
 */
@Getter
public enum SteerSide {

    PORT(-1),
    STARBOARD(1);

    private final int netInput;

    SteerSide(int netInput) {
        this.netInput = netInput;
    }

    public SteerSide opposite() {
        return this == PORT ? STARBOARD : PORT;
    }
}
