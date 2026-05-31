package me.mykindos.betterpvp.core.world.zone;

import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;

/**
 * A rule attached directly to a {@link Zone} (the composition half of the zone rule system). Each rule inspects a
 * {@link ZoneActionContext} and returns whether the action should be allowed, denied, or left to default.
 * <p>
 * Concrete rules live in consuming modules, not the framework: e.g. clans attach a rule denying block breaks to
 * non-members, an environment module attaches one enabling damage in water. The framework only provides the seam.
 *
 * @see ZoneInteractEvent for the decoupled, event-bus alternative
 */
@FunctionalInterface
public interface ZoneRule {

    /**
     * @param context the action being evaluated
     * @return {@link Event.Result#DENY} to block, {@link Event.Result#ALLOW} to explicitly permit, or
     * {@link Event.Result#DEFAULT} to abstain
     */
    @NotNull Event.Result evaluate(@NotNull ZoneActionContext context);
}
