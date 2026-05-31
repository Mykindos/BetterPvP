package me.mykindos.betterpvp.core.world.zone;

/**
 * Static home for reusable, world-independent zone building blocks, mirroring the role {@code SocketableGroups} plays
 * for socketables. Today it holds the well-known {@link Zone#getTags() tags} that consumers attach to zones and that
 * {@link ZoneRule}s and {@link ZoneInteractEvent} handlers branch on; as shared ambient {@link Zone} instances (e.g. a
 * global "in water" zone) are introduced they will live here too.
 * <p>
 * Tags are intentionally plain strings so any module can define its own without a central registry; these are simply
 * the cross-cutting ones worth standardising on.
 */
public final class Zones {

    private Zones() {
    }

    /** Marks a zone as safe: combat/damage is expected to be suppressed. */
    public static final String SAFE = "safe";

    /** Marks a zone as PvP-enabled. */
    public static final String PVP = "pvp";

    /** Marks a zone where block placement and breaking should be denied by default. */
    public static final String NO_BUILD = "no_build";
}
