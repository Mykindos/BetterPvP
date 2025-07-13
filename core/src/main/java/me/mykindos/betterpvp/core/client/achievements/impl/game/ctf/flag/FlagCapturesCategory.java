package me.mykindos.betterpvp.core.client.achievements.impl.game.ctf.flag;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.achievements.category.AchievementCategories;
import me.mykindos.betterpvp.core.client.achievements.category.AchievementCategory;
import me.mykindos.betterpvp.core.client.achievements.category.SubCategory;
import me.mykindos.betterpvp.core.client.achievements.category.types.GameCategory;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import org.bukkit.Material;

@Singleton
/**
 * The category, which defines the types and display view
 */
@SubCategory(GameCategory.class)
public class FlagCapturesCategory extends AchievementCategory {
    public FlagCapturesCategory() {
        super(AchievementCategories.GAME_FLAG_CAPTURES);
        setParent(AchievementCategories.GAME);
    }


    @Override
    public ItemView getItemView() {
        return ItemView.builder()
                .material(Material.WHITE_BANNER)
                .displayName(UtilMessage.deserialize("<white>Flag Captures"))
                .build();
    }
}
