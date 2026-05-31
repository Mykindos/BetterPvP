package me.mykindos.betterpvp.core.world.zone;

import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Holds the ordered {@link ZoneRule}s attached to a {@link Zone} and resolves them into a single verdict.
 * <p>
 * Backed by a copy-on-write list: rules are added rarely (at setup) and iterated on the hot path (per action), so
 * reads are lock-free and a concurrent add never disturbs an in-flight evaluation.
 */
public final class ZoneRuleContainer {

    private final List<ZoneRule> rules = new CopyOnWriteArrayList<>();

    /**
     * @param rule the rule to attach
     * @return this container, for chaining
     */
    public ZoneRuleContainer add(@NotNull ZoneRule rule) {
        rules.add(rule);
        return this;
    }

    public boolean remove(@NotNull ZoneRule rule) {
        return rules.remove(rule);
    }

    public boolean isEmpty() {
        return rules.isEmpty();
    }

    public @NotNull @Unmodifiable List<ZoneRule> getRules() {
        return Collections.unmodifiableList(rules);
    }

    /**
     * Evaluates every rule and collapses them into one result with <b>deny-overrides</b> precedence: a single
     * {@link Event.Result#DENY} short-circuits and wins outright; otherwise an explicit {@link Event.Result#ALLOW}
     * sticks; with no opinion at all the result is {@link Event.Result#DEFAULT}, leaving the decision to whatever the
     * caller's default behaviour is. This makes protective rules (deny) authoritative over permissive ones (allow),
     * which is the safe default for territory-style protection.
     *
     * @param context the action being evaluated
     * @return the collapsed verdict
     */
    public @NotNull Event.Result evaluate(@NotNull ZoneActionContext context) {
        Event.Result result = Event.Result.DEFAULT;
        for (ZoneRule rule : rules) {
            final Event.Result verdict = rule.evaluate(context);
            if (verdict == Event.Result.DENY) {
                return Event.Result.DENY;
            }
            if (verdict == Event.Result.ALLOW) {
                result = Event.Result.ALLOW;
            }
        }
        return result;
    }
}
