package me.mykindos.betterpvp.core.client.stats.formatter;

import me.mykindos.betterpvp.core.client.stats.IStat;
import me.mykindos.betterpvp.core.client.stats.StatContainer;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.description.Description;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.jetbrains.annotations.Nullable;

public interface IStatFormatter {
    @Nullable
    String getCategory();


    String getStatType();

    /**
     * Get the stat this formatter represents.
     * @return the {@link IStat} or {@code null} if this a fallback generic
     */
    @Nullable
    IStat getStat();

    /**
     * Get the description for the stat
     * @param statName the statName
     * @param stat the stat value
     * @return
     */
    default Description getDescription(String statName, StatContainer container, String period) {
        ItemView itemView = ItemView.builder()
                .displayName(Component.text(statName))
                .lore(UtilMessage.deserialize("<white>Value</white>: <green>%s</green>", getStat().getStat(container, period)))
                .frameLore(true)
                .material(Material.PAPER)
                .build();
        return Description.builder()
                .icon(itemView)
                .build();
    }

}
