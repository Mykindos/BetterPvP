package me.mykindos.betterpvp.core.client.achievements.category.types;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.achievements.category.AchievementCategories;
import me.mykindos.betterpvp.core.client.achievements.category.AchievementCategory;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;

import java.util.List;

@Singleton
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
                .displayName(Translations.component("core.achievement.category.events.name").color(NamedTextColor.WHITE))
                .lore(
                        List.of(Translations.component("core.achievement.category.events.description"))
                )
                .material(Material.BEACON)
                .build();
    }
}
