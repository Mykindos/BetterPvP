package me.mykindos.betterpvp.core.client.achievements.category;

import me.mykindos.betterpvp.core.framework.manager.Manager;
import org.bukkit.NamespacedKey;

public class AchievementCategoryManager extends Manager<IAchievementCategory> {
    public void addObject(NamespacedKey identifier, IAchievementCategory object) {
        super.addObject(identifier.asString(), object);
    }
}
