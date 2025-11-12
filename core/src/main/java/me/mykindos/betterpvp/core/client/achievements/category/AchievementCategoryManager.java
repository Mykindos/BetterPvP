package me.mykindos.betterpvp.core.client.achievements.category;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.framework.manager.Manager;
import org.bukkit.NamespacedKey;

@Singleton
public class AchievementCategoryManager extends Manager<String, IAchievementCategory> {
    public void addObject(NamespacedKey identifier, IAchievementCategory object) {
        super.addObject(identifier.asString(), object);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("AchievementCategories: ");
        getObjects().values().forEach(category -> {
            builder.append("(").append(category.toString()).append(") ");
        });
        return builder.toString();
    }
}
