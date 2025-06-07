package me.mykindos.betterpvp.core.client.stats.display;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import me.mykindos.betterpvp.core.client.stats.ClientStat;
import me.mykindos.betterpvp.core.client.stats.IClientStat;
import me.mykindos.betterpvp.core.client.stats.StatContainer;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface IStatItemView {
    /**
     * Generates the item view representing a stat {@link ClientStat}
     * @param viewer the viewer viewing this stat
     * @param stat the stat this view is for
     * @param statContainer the stat container this view is for
     * @param period the period this view is for
     * @return the {@link ItemView} representing this stat
     */
    ItemView getItemView(@NotNull Audience viewer,
                                @NotNull Enum<? extends IClientStat> stat,
                                @NotNull StatContainer statContainer,
                                @NotNull String period);
//todo retarget to just IClientStat?
    IStatItemView generalStat = (@NotNull Audience viewer, @NotNull Enum<? extends IClientStat> stat, @NotNull StatContainer statContainer, @NotNull String period) -> {
        IClientStat statData = (IClientStat) stat;
        List<Component> lore = new ArrayList<>(Arrays.stream(statData.getDescription()).map(UtilMessage::deserialize).toList());
        lore.add(Component.empty());
        lore.add(UtilMessage.deserialize("<white>Value</white>: <green>%s</green>", statContainer.getProperty(statData.name(), period)));
        return ItemView.builder()
                .displayName(Component.text(statData.getName()))
                .lore(lore)
                .frameLore(true)
                .material(Material.BOOK)
                .build();
    };

}
