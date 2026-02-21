package me.mykindos.betterpvp.core.client.achievements.category.types;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.achievements.category.AchievementCategories;
import me.mykindos.betterpvp.core.client.achievements.category.AchievementCategory;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
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
                .displayName(UtilMessage.deserialize("<white>Game"))
                .build();
    }
}
