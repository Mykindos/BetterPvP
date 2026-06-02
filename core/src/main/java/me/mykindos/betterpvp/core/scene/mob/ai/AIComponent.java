package me.mykindos.betterpvp.core.scene.mob.ai;

import java.util.EnumSet;

/**
 * A single, self-contained piece of mob behaviour - the composition unit of the AI system
 * (wander, follow owner, melee attack, target selection, ...). Components are attached to a
 * {@link me.mykindos.betterpvp.core.scene.mob.SceneMob} and arbitrated by the {@link AIController},
 * which decides which components run based on their priority and the {@link AIControl}s they claim.
 * <p>
 * A component does <b>not</b> declare its own priority; the {@link AIController} assigns it from the
 * order components are added (earlier-added components have higher priority and win shared controls).
 * This keeps components position-agnostic and reusable across mobs.
 * <p>
 * Lifecycle (driven by {@link AIController}):
 * <ol>
 *   <li>{@link #canStart()} is polled; when it returns {@code true} and the component's controls
 *       are free (or held only by lower-priority components), {@link #start()} is called.</li>
 *   <li>{@link #tick()} runs every tick while the component is active.</li>
 *   <li>When {@link #shouldContinue()} returns {@code false}, or a higher-priority component
 *       preempts a shared control, {@link #stop()} is called.</li>
 * </ol>
 * A component holds whatever state it needs (typically a reference to its {@code SceneMob}).
 */
public interface AIComponent {

    /**
     * Controls this component needs to run. Two components that share a control cannot run
     * simultaneously - the higher-priority one wins. An empty set means the component never
     * conflicts (pure logic).
     */
    EnumSet<AIControl> getControls();

    /** @return whether this component is eligible to start right now. */
    boolean canStart();

    /** @return whether an already-running component should keep running. Defaults to {@link #canStart()}. */
    default boolean shouldContinue() {
        return canStart();
    }

    /** Called when the component becomes active. */
    default void start() {}

    /** Called every tick while the component is active. */
    void tick();

    /** Called when the component stops (no longer continuing, or preempted). */
    default void stop() {}

}
