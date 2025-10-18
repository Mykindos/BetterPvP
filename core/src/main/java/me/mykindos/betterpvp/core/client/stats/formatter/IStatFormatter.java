package me.mykindos.betterpvp.core.client.stats.formatter;

import me.mykindos.betterpvp.core.client.stats.StatContainer;
import me.mykindos.betterpvp.core.client.stats.impl.IStat;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.description.Description;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface IStatFormatter {
    @Nullable
    String getCategory();


    @Nullable
    String getStatType();

    /**
     * Get the stat this formatter represents.
     * @return the {@link IStat} or {@code null} if this a fallback generic
     */
    @Nullable
    IStat getStat();

    //todo split out the components of the itemview/description to allow for easier overrides in child classes

    default List<Component> getLore(IStat stat, StatContainer container, String period) {
        Double value = container.getProperty(period, stat);
        if (getStat() != null) {
            value = getStat().getStat(container, period);
        }
        return List.of(UtilMessage.deserialize("<white>Value</white>: <green>%s</green>", value));

    }

    default Component getDisplayName(IStat stat, StatContainer container, String period) {
        return Component.text(stat.getStatName());
    }

    default Material getMaterial(IStat stat, StatContainer container, String period) {
        return Material.PAPER;
    }

    default int getCustomModelData(IStat stat, StatContainer container, String period) {
        return 0;
    }

    default boolean getGlow(IStat stat, StatContainer container, String period) {
        return false;
    }


    /**
     * Get the description for the stat
     * @param statName the statName
     * @param stat the stat value
     * @return
     */
    default Description getDescription(IStat stat, StatContainer container, String period) {

        ItemView itemView = ItemView.builder()
                .displayName(getDisplayName(stat, container, period))
                .lore(getLore(stat, container, period))
                .frameLore(true)
                .material(getMaterial(stat, container, period))
                .customModelData(getCustomModelData(stat, container, period))
                .glow(getGlow(stat, container, period))
                .build();
        return Description.builder()
                .icon(itemView)
                .build();
    }

}
