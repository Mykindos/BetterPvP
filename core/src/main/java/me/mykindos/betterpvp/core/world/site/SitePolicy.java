package me.mykindos.betterpvp.core.world.site;

import lombok.Builder;
import lombok.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * How a site behaves. Covers how many instances of it exist, who may share one, where a player returns to, and what
 * happens to an instance nobody is standing in.
 */
@Value
@Builder
public class SitePolicy {

    public enum Lifecycle {
        /** Exactly one instance, always. */
        PERMANENT,
        /** Between {@code min} and {@code max} instances, kept warm and never reaped. */
        POOLED,
        /** One per visiting party, reaped once empty. */
        ON_DEMAND,
        /** Exactly one per owner, kept for as long as the owner exists. */
        OWNED
    }

    public enum Rejoin {
        /** Back to the instance they left, if it still exists. */
        RESUME,
        /** A new instance of the same site. */
        REINSTANCE,
        /** Somewhere else entirely, being their anchor. */
        ANCHOR
    }

    public enum RejoinAt {
        /** Exactly where they logged out. */
        EXACT,
        /** The world's spawn point, wherever they were standing. */
        SPAWN_POINT
    }

    public enum Dormancy {
        /** The world stays loaded whether or not anybody is in it. */
        ALWAYS_LOADED,
        /** The world unloads once empty and is woken on the next arrival. The folder is kept. */
        UNLOAD_WHEN_EMPTY
    }

    @Builder.Default
    @NotNull Lifecycle lifecycle = Lifecycle.PERMANENT;

    /** Instances provisioned ahead of demand, so a party arriving never waits on a world being made. */
    @Builder.Default
    int min = 0;

    /** A ceiling on live instances. Zero means no ceiling. */
    @Builder.Default
    int max = 0;

    /** Occupants one instance will hold. Zero means no limit. */
    @Builder.Default
    int capacity = 0;

    @Builder.Default
    @NotNull Admission admission = Admission.open();

    @Builder.Default
    @NotNull Selection selection = Selection.fillFirst();

    @Builder.Default
    @NotNull Rejoin rejoin = Rejoin.ANCHOR;

    @Builder.Default
    @NotNull RejoinAt rejoinAt = RejoinAt.SPAWN_POINT;

    /** Whether a player who has been here can be sent back here as a fallback. */
    @Builder.Default
    boolean anchorable = false;

    @Builder.Default
    @NotNull Dormancy dormancy = Dormancy.ALWAYS_LOADED;

    /** How long an empty instance waits before going dormant. */
    @Builder.Default
    int dormancyGraceSeconds = 300;

    /** Where a party goes when this site is full and may not grow. */
    @Nullable String fallbackSiteId;

    /** The server that hosts this site, or {@code null} for whichever one is running. */
    @Nullable String server;

    /**
     * Whether an empty instance of this site should be destroyed outright.
     */
    public boolean reapsWhenEmpty() {
        return lifecycle == Lifecycle.ON_DEMAND;
    }
}
