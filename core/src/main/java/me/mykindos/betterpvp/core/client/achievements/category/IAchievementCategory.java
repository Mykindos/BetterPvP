package me.mykindos.betterpvp.core.client.achievements.category;

import java.util.Collection;
import java.util.Set;
import me.mykindos.betterpvp.core.properties.PropertyContainer;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface IAchievementCategory {
    /**
     * Get the key of this category
     * @return
     */
    @NotNull
    NamespacedKey getNamespacedKey();

    @Nullable
    NamespacedKey getParent();

    void setParent(NamespacedKey namespacedKey);

    /**
     * Add a child to this category
     * @param child
     */
    void addChild(IAchievementCategory child);


    /**
     * Gets the children of this category
     * @return
     */
    Collection<IAchievementCategory> getChildren();


    /**
     * Get the set of Classes of containers that use this category
     * @return
     */
    Set<Class<? extends PropertyContainer>> allowedTypes();


    /**
     * Check if this category is used for this container
     * @param container
     * @return
     */
    default boolean isAllowed(PropertyContainer container) {
        return allowedTypes().stream()
                .anyMatch(clazz -> clazz.isInstance(container));
    }

    ItemView getItemView();
}
