package me.mykindos.betterpvp.core.scene.mob.ai;

/**
 * Mutually-exclusive control "slots" an {@link AIComponent} can claim, mirroring vanilla
 * Minecraft's goal flags. The {@link AIController} guarantees that at most one running component
 * owns a given control at a time; a higher-priority component preempts a lower-priority one that
 * holds a control it needs.
 */
public enum AIControl {
    /** Controls the entity's movement / navigation. */
    MOVE,
    /** Controls where the entity is looking. */
    LOOK,
    /** Controls target acquisition. */
    TARGET,
    /** Controls jumping. */
    JUMP
}
