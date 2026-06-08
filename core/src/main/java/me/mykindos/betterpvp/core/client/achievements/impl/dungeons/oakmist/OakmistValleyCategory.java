package me.mykindos.betterpvp.core.client.achievements.impl.dungeons.oakmist;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.achievements.category.AchievementCategories;
import me.mykindos.betterpvp.core.client.achievements.category.AchievementCategory;
import me.mykindos.betterpvp.core.client.achievements.category.SubCategory;
import me.mykindos.betterpvp.core.client.achievements.category.types.DungeonsCategory;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.format.NamedTextColor;
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
                .displayName(Translations.component("core.achievement.category.oakmist-valley.name").color(NamedTextColor.WHITE))
                .build();
    }
}

