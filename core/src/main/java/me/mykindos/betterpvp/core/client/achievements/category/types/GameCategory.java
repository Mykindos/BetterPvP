package me.mykindos.betterpvp.core.client.achievements.category.types;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.achievements.category.AchievementCategories;
import me.mykindos.betterpvp.core.client.achievements.category.AchievementCategory;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;

@Singleton
/**
 * The category, which defines the types and display view
 */
public class GameCategory extends AchievementCategory {
    public GameCategory() {
        super(AchievementCategories.GAME);
    }

    @Override
    public ItemView getItemView() {
        return ItemView.builder()
                .material(Material.BLAZE_ROD)
                .displayName(Translations.component("core.achievement.category.game.name").color(NamedTextColor.WHITE))
                .build();
    }
}
