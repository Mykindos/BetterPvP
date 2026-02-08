package me.mykindos.betterpvp.core.utilities.model.selector.entity;

import me.mykindos.betterpvp.core.utilities.model.selector.origin.SelectorOrigin;
import org.bukkit.entity.LivingEntity;

/**
 * A composable filter for determining whether entities should be selected.
 * Filters can be combined using logical AND, OR, and NOT operations.
 */
@FunctionalInterface
public interface EntityFilter {

    /**
     * Tests whether the given entity passes this filter.
     *
     * @param origin the origin of the selection
     * @param entity the entity to test
     * @return true if the entity passes the filter, false otherwise
     */
    boolean test(SelectorOrigin origin, LivingEntity entity);

    /**
     * Creates a filter that passes only if both this filter and the other filter pass.
     *
     * @param other the other filter
     * @return the combined filter using AND logic
     */
    default EntityFilter and(EntityFilter other) {
        return (origin, entity) -> this.test(origin, entity) && other.test(origin, entity);
    }

    /**
     * Creates a filter that passes if either this filter or the other filter passes.
     *
     * @param other the other filter
     * @return the combined filter using OR logic
     */
    default EntityFilter or(EntityFilter other) {
        return (origin, entity) -> this.test(origin, entity) || other.test(origin, entity);
    }

    /**
     * Creates a filter that passes only if this filter does not pass.
     *
     * @return the negated filter
     */
    default EntityFilter negate() {
        return (origin, entity) -> !this.test(origin, entity);
    }

    /**
     * Returns a filter that always passes.
     *
     * @return a filter that accepts all entities
     */
    static EntityFilter all() {
        return (origin, entity) -> true;
    }

    /**
     * Returns a filter that never passes.
     *
     * @return a filter that rejects all entities
     */
    static EntityFilter none() {
        return (origin, entity) -> false;
    }
}
