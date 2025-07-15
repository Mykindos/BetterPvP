package me.mykindos.betterpvp.core.client.achievements.category.types;

import me.mykindos.betterpvp.core.client.achievements.category.AchievementCategories;
import me.mykindos.betterpvp.core.client.achievements.category.AchievementCategory;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;

import java.util.List;

public class EventCategory extends AchievementCategory {
    public EventCategory() {
        super(AchievementCategories.EVENT);
    }

    /**
     * Get the set of Classes of containers that use this category
     *
     * @return
     */

    @Override
    public ItemView getItemView() {
        return ItemView.builder()
                .displayName(Component.text("Events"))
                .lore(
                        List.of(Component.text("Event related achievements"))
                )
                .material(Material.BEACON)
                .build();
    }
}
