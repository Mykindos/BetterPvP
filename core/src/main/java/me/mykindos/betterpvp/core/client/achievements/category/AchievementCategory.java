package me.mykindos.betterpvp.core.client.achievements.category;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.client.achievements.IAchievement;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a category of an {@link IAchievement}
 */
@Getter
public abstract class AchievementCategory implements IAchievementCategory {
    private final NamespacedKey namespacedKey;
    @Setter
    @Nullable
    private NamespacedKey parent = null;
    private final Collection<IAchievementCategory> children = new HashSet<>();

    public AchievementCategory(NamespacedKey namespacedKey) {
        this.namespacedKey = namespacedKey;
    }
    public void addChild(IAchievementCategory child) {
        children.add(child);
    }


    @Override
    public boolean equals(Object o) {
        if (!(o instanceof AchievementCategory that)) return false;
        return Objects.equals(namespacedKey, that.namespacedKey);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(namespacedKey);
    }

    @Override
    public String toString() {
        return "AchievementCategory{" +
                "namespacedKey=" + namespacedKey +
                ", parent=" + parent +
                ", children=" + children +
                '}';
    }
}
