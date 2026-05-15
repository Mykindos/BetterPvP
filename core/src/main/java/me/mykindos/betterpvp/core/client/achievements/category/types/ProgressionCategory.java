package me.mykindos.betterpvp.core.client.achievements.category.types;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.achievements.category.AchievementCategories;
import me.mykindos.betterpvp.core.client.achievements.category.AchievementCategory;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;

import java.util.List;

@Singleton
public class ProgressionCategory extends AchievementCategory {

    @Inject
    public ProgressionCategory() {
        super(AchievementCategories.PROGRESSION);
    }

    @Override
    public ItemView getItemView() {
        return ItemView.builder()
                .displayName(Component.text("Professions"))
                .lore(List.of(Component.text("Profession related achievements")))
                .material(Material.EXPERIENCE_BOTTLE)
                .build();
    }
}

