package me.mykindos.betterpvp.core.client.stats.formatter.category;

import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.description.Description;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;

public class ChampionsSkillCategory extends StatCategory {

    public ChampionsSkillCategory() {
        super("Champions Skill Stats");
    }

    /**
     * @return the description of this entity
     */
    @Override
    public Description getDescription() {
        ItemView itemView = ItemView.builder()
                .displayName(Component.text(getName()))
                .lore(UtilMessage.deserialize("Champions Skill Stats"))
                .material(Material.ENCHANTING_TABLE)
                .build();
        return Description.builder()
                .icon(itemView)
                .build();
    }
}
