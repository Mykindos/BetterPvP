package me.mykindos.betterpvp.core.world.settler;

/** When a trait does anything. */
public enum TraitGroup {
    /** Only while its settler is on a construction job. */
    BUILDER,
    /** Only while its settler works at a workplace. */
    RESIDENT,
    /** From anywhere, for the whole site. */
    SITE_WIDE,
    /** On its settler alone. */
    PERSONAL
}
