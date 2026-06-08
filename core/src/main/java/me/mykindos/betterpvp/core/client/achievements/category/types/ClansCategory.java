package me.mykindos.betterpvp.core.client.achievements.category.types;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.achievements.category.AchievementCategories;
import me.mykindos.betterpvp.core.client.achievements.category.AchievementCategory;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;

import java.util.List;

@Singleton
public class ClansCategory extends AchievementCategory {
    @Inject
    public ClansCategory() {
        super(AchievementCategories.CLANS);
    }

    @Override
    public ItemView getItemView() {
        return ItemView.builder()
                .displayName(Translations.component("core.achievement.category.clans.name").color(NamedTextColor.WHITE))
                .lore(
                        List.of(Translations.component("core.achievement.category.clans.description"))
                )
                .material(Material.DIAMOND_SWORD)
                .build();
    }
}
