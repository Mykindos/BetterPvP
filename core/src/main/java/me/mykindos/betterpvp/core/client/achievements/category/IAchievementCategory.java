package me.mykindos.betterpvp.core.client.achievements.category;

import java.util.Collection;
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

    ItemView getItemView();
}
