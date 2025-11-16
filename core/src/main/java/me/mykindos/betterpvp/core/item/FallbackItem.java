package me.mykindos.betterpvp.core.item;

import org.bukkit.Material;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as a fallback item definition.
 * <p>
 * Fallback items are used when no custom item with the same {@link ItemKey}
 * is registered or when default material-based items must remain available.
 * </p>
 *
 * <p><b>Usage example:</b></p>
 * <pre>
 * &#64;FallbackItem(value = Material.IRON_SWORD, keepRecipe = true)
 * public class StandardSword extends BaseItem {
 *     // item logic
 * }
 * </pre>
 *
 * <p>If {@code keepRecipe} is set to {@code true}, the original Minecraft recipe
 * for the material will be preserved instead of being replaced.</p>
 *
 * @see ItemKey
 * @see me.mykindos.betterpvp.core.item.ItemRegistry
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface FallbackItem {

    /**
     * The base {@link Material} of the fallback item.
     *
     * @return material used for the fallback item
     */
    Material value();

    /**
     * Whether the default crafting recipe should be retained.
     * <p>Defaults to {@code false} (recipe will be removed or overridden).</p>
     *
     * @return true if the recipe should be kept
     */
    boolean keepRecipes() default false;
}
