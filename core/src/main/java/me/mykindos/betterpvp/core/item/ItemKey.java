package me.mykindos.betterpvp.core.item;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used to uniquely identify an item type within the BetterPvP framework.
 * <p>
 * Each annotated class represents a distinct item and must define a {@code namespace}
 * and {@code key} pair that together form a unique identifier.
 * </p>
 *
 * <p><b>Usage example:</b></p>
 * <pre>
 * &#64;ItemKey(namespace = "core", key = "standard_sword")
 * public class StandardSword extends BaseItem {
 *     // item logic
 * }
 * </pre>
 *
 * <p>The identifier generated from this annotation (e.g. {@code core:standard_sword})
 * is used internally for registration, serialization, and lookup in the {@link ItemRegistry}.
 * </p>
 *
 * @see me.mykindos.betterpvp.core.item.ItemRegistry
 * @see me.mykindos.betterpvp.core.item.BaseItem
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ItemKey {

    /**
     * The namespaced key of the item. If a namespaced is not included, a default
     * <code>minecraft</code> one will be used.
     *
     * <p>
     *     Examples:
     *     <ul>
     *         <li><code>minecraft:stone</code></li>
     *         <li><code>core:apple</code></li>
     *         <li><code>champions:pickaxe</code></li>
     *         <li><code>progression:fish</code></li>
     *     </ul>
     * </p>
     *
     * @return the unique key identifying the item
     */
    String value();
}
