package me.mykindos.betterpvp.clans.world.discovery;

import lombok.Value;

/**
 * The handling knobs of a steered ship: how fast the wheel turns, how hard the hull follows it, and what it costs in
 * speed.
 * <p>
 * Separated from {@link ShipDynamics} so the feel of a vessel is data rather than code — a longboat and a galleon differ
 * only in these numbers — and so the motion itself can be exercised against deliberately extreme values.
 */
@Value
public class ShipDynamicsConfig {

    /** Handling for a vessel that has not said otherwise. */
    public static final ShipDynamicsConfig DEFAULT = new ShipDynamicsConfig(8.0, 0.9, 0.12, 0.65, 14.0, 1.6, 0.35);

    /** Blocks per second with the rudder centred and a neutral wind. */
    double baseSpeed;

    /** Rudder units per second gained while a control is held, measured at centre. */
    double rudderInRate;

    /** Rudder units per second shed once the controls are released. */
    double rudderOutRate;

    /**
     * Fraction of {@link #rudderInRate} lost at full deflection, in {@code [0, 1]}. This is what gives the wheel its
     * weight: quick through the middle, crawling against the stops.
     */
    double rudderResistance;

    /** Degrees per second of turn the hull settles at with the rudder hard over. */
    double maxYawRate;

    /** Per-second coefficient for how quickly the hull's turn catches up to the rudder. Higher is twitchier. */
    double hullResponse;

    /** Fraction of {@link #baseSpeed} lost at full deflection, in {@code [0, 1]}. Turning hard should cost way. */
    double turnDrag;
}
