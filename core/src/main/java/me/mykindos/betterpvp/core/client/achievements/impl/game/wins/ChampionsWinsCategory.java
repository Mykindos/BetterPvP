package me.mykindos.betterpvp.core.client.achievements.impl.game.wins;

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
public class ChampionsWinsCategory extends AchievementCategory {
    public ChampionsWinsCategory() {
        super(AchievementCategories.GAME_CHAMPIONS_WINS);
    }


    @Override
    public ItemView getItemView() {
        return ItemView.builder()
                .material(Material.BELL)
                .displayName(UtilMessage.deserialize("<white>Champions Wins"))
                .build();
    }
}
