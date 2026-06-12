package me.mykindos.betterpvp.core.client.achievements.category.types;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import me.mykindos.betterpvp.core.client.achievements.category.AchievementCategories;
import me.mykindos.betterpvp.core.client.achievements.category.AchievementCategory;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;

@Singleton
public class CombatCategory extends AchievementCategory {
    @Inject
    public CombatCategory() {
        super(AchievementCategories.COMBAT_CATEGORY);
    }

    /**
     * Gets a mini-message formatted title for this category via {@link UtilMessage#deserialize(String)}
     *
     * @return the formatted title
     */

    @Override
    public ItemView getItemView() {
        return ItemView.builder()
                .displayName(Translations.component("core.achievement.category.combat.name").color(NamedTextColor.WHITE))
                .lore(
                        List.of(Translations.component("core.achievement.category.combat.description"))
                )
                .material(Material.IRON_SWORD)
                .build();
    }
}
