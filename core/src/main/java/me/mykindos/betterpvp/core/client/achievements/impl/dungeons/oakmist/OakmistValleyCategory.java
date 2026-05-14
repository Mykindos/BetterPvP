package me.mykindos.betterpvp.core.client.achievements.impl.dungeons.oakmist;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.achievements.category.AchievementCategories;
import me.mykindos.betterpvp.core.client.achievements.category.AchievementCategory;
import me.mykindos.betterpvp.core.client.achievements.category.SubCategory;
import me.mykindos.betterpvp.core.client.achievements.category.types.DungeonsCategory;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import org.bukkit.Material;

@Singleton
@SubCategory(DungeonsCategory.class)
public class OakmistValleyCategory extends AchievementCategory {

    public OakmistValleyCategory() {
        super(AchievementCategories.DUNGEONS_OAKMIST_VALLEY_PERIOD);
    }

    @Override
    public ItemView getItemView() {
        return ItemView.builder()
                .material(Material.OAK_LEAVES)
                .displayName(UtilMessage.deserialize("<white>Defeat Oakmist Valley"))
                .build();
    }
}

