package me.mykindos.betterpvp.core.utilities.model.selector.entity;

import me.mykindos.betterpvp.core.utilities.model.selector.Selector;
import me.mykindos.betterpvp.core.utilities.model.selector.origin.SelectorOrigin;
import org.bukkit.entity.LivingEntity;

/**
 * A selector specialized for selecting living entities.
 * Provides additional methods for filtering and origin manipulation.
 *
 * @param <E> the type of entity to select, must extend LivingEntity
 */
public interface EntitySelector<E extends LivingEntity> extends Selector<E> {

    /**
     * Gets the origin of this selector.
     *
     * @return the selector origin
     */
    SelectorOrigin getOrigin();

    /**
     * Creates a new selector with an additional filter applied.
     * The filter will be combined with existing filters using AND logic.
     *
     * @param filter the filter to add
     * @return a new selector with the filter applied
     */
    EntitySelector<E> withFilter(EntityFilter filter);

    /**
     * Creates a new selector with a different origin.
     *
     * @param origin the new origin
     * @return a new selector with the specified origin
     */
    EntitySelector<E> withOrigin(SelectorOrigin origin);
}
