package me.mykindos.betterpvp.core.client.achievements.impl.events.bosses.deepcreature;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.achievements.category.AchievementCategories;
import me.mykindos.betterpvp.core.client.achievements.category.AchievementCategory;
import me.mykindos.betterpvp.core.client.achievements.category.SubCategory;
import me.mykindos.betterpvp.core.client.achievements.category.types.EventCategory;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;

@Singleton
@SubCategory(EventCategory.class)
public class DeepCreatureKillCategory extends AchievementCategory {

    public DeepCreatureKillCategory() {
        super(AchievementCategories.EVENT_BOSS_DEEP_CREATURE);
    }

    @Override
    public ItemView getItemView() {
        return ItemView.builder()
                .material(Material.PRISMARINE)
                .displayName(Translations.component("core.achievement.category.deep-creature.name").color(NamedTextColor.WHITE))
                .build();
    }
}

