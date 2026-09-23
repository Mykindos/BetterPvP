package me.mykindos.betterpvp.core.world.construction;

import lombok.Builder;
import lombok.Value;

/** What a structure type allows and how it starts out. */
@Value
@Builder
public class StructureFlags {

    @Builder.Default
    boolean movable = true;

    @Builder.Default
    boolean demolishable = true;

    /** Repairs itself after being disabled, without a job. */
    boolean selfRepairing;

    /** Usable by visitors who are not members. */
    boolean publicUse;

    /** Placed already needing a repair, rather than working. */
    boolean startsBroken;

    /** The share of what was spent on it that demolishing gives back, from 0 to 1. */
    double demolishRefund;
}
