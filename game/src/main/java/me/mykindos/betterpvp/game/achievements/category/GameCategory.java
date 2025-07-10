package me.mykindos.betterpvp.game.achievements.category;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.achievements.category.AchievementCategory;
import me.mykindos.betterpvp.core.client.achievements.category.SubCategory;
import me.mykindos.betterpvp.core.client.achievements.category.types.CombatCategory;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import org.bukkit.Material;

@Singleton
/**
 * The category, which defines the types and display view
 */
@SubCategory(CombatCategory.class)
public class GameCategory extends AchievementCategory {
    public GameCategory() {
        super(GameAchievementCategories.GAME);
    }

    @Override
    public ItemView getItemView() {
        return ItemView.builder()
                .material(Material.BLAZE_ROD)
                .displayName(UtilMessage.deserialize("<white>Game"))
                .build();
    }
}
