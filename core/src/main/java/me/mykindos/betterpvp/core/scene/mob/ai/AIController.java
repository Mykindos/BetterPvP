package me.mykindos.betterpvp.core.scene.mob.ai;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The mob's "custom ticker": a priority/controls arbiter modelled on vanilla's GoalSelector but
 * implemented as plain code we own. Each tick it stops components that no longer want to run,
 * starts eligible components (preempting lower-priority holders of a shared {@link AIControl}),
 * then ticks everything currently running.
 * <p>
 * Priority is <b>positional</b>: components are kept in an ordered list and a component's priority is
 * simply its index, where index 0 is the highest priority (wins shared controls). Components do not
 * declare their own priority - it is assigned by the order they are added. Use {@link #add} to append
 * at the lowest priority, {@link #addFirst} for the highest, or {@link #addBefore}/{@link #addAfter}
 * to splice one into the middle relative to an already-registered component.
 * <p>
 * Not thread-safe; ticked from the main server thread by
 * {@link me.mykindos.betterpvp.core.scene.mob.SceneMob#tick()}.
 */
public class AIController {

    private final List<AIComponent> components = new ArrayList<>();
    private final Set<AIComponent> running = new HashSet<>();
    private final Map<AIControl, AIComponent> controlOwners = new EnumMap<>(AIControl.class);

    /** Appends a component at the lowest priority - it runs only when nothing above it claims its controls. */
    public void add(AIComponent component) {
        components.add(component);
    }

    /** Inserts a component at the highest priority, ahead of everything already registered. */
    public void addFirst(AIComponent component) {
        components.addFirst(component);
    }

    /**
     * Inserts {@code component} immediately above (higher priority than) {@code reference}. Falls back
     * to appending at the lowest priority if {@code reference} is not registered.
     */
    public void addBefore(AIComponent reference, AIComponent component) {
        final int index = components.indexOf(reference);
        components.add(index < 0 ? components.size() : index, component);
    }

    /**
     * Inserts {@code component} immediately below (lower priority than) {@code reference}. Falls back
     * to appending at the lowest priority if {@code reference} is not registered.
     */
    public void addAfter(AIComponent reference, AIComponent component) {
        final int index = components.indexOf(reference);
        components.add(index < 0 ? components.size() : index + 1, component);
    }

    public void tick() {
        // 1. Stop running components that no longer want to continue.
        for (AIComponent component : List.copyOf(running)) {
            if (!component.shouldContinue()) {
                stopComponent(component);
            }
        }

        // 2. Start eligible components in priority order, preempting lower-priority control holders.
        for (AIComponent component : components) {
            if (running.contains(component) || !component.canStart() || !controlsAvailable(component)) {
                continue;
            }
            for (AIControl control : component.getControls()) {
                final AIComponent owner = controlOwners.get(control);
                if (owner != null) {
                    stopComponent(owner);
                }
            }
            startComponent(component);
        }

        // 3. Tick everything currently running.
        for (AIComponent component : List.copyOf(running)) {
            component.tick();
        }
    }

    /** A control is available if it is free or held only by a strictly-lower-priority component. */
    private boolean controlsAvailable(AIComponent candidate) {
        for (AIControl control : candidate.getControls()) {
            final AIComponent owner = controlOwners.get(control);
            if (owner != null && priorityOf(owner) <= priorityOf(candidate)) {
                return false;
            }
        }
        return true;
    }

    /** A component's priority is its position in the list; a lower index means a higher priority. */
    private int priorityOf(AIComponent component) {
        return components.indexOf(component);
    }

    private void startComponent(AIComponent component) {
        component.start();
        running.add(component);
        for (AIControl control : component.getControls()) {
            controlOwners.put(control, component);
        }
    }

    private void stopComponent(AIComponent component) {
        component.stop();
        running.remove(component);
        for (AIControl control : component.getControls()) {
            controlOwners.remove(control, component);
        }
    }

    /** Stops every running component (used when the mob is frozen or removed). */
    public void stopAll() {
        for (AIComponent component : List.copyOf(running)) {
            stopComponent(component);
        }
    }

}
