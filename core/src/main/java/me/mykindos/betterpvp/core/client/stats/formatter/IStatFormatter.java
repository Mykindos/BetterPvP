package me.mykindos.betterpvp.core.client.stats.formatter;

import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.description.Description;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;

public interface IStatFormatter {
    String getStatType();

    /**
     * Get the description for the stat
     * @param statName the statName
     * @param stat the stat value
     * @return
     */
    default Description getDescription(String statName, Double stat) {
        ItemView itemView = ItemView.builder()
                .displayName(Component.text(statName))
                .lore(UtilMessage.deserialize("<white>Value</white>: <green>%s</green>", stat))
                .frameLore(true)
                .material(Material.PAPER)
                .build();
        return Description.builder()
                .icon(itemView)
                .build();
    }

}
