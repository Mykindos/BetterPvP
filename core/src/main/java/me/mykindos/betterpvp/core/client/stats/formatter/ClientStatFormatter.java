package me.mykindos.betterpvp.core.client.stats.formatter;

import me.mykindos.betterpvp.core.client.stats.StatContainer;
import me.mykindos.betterpvp.core.client.stats.impl.IClientStat;
import me.mykindos.betterpvp.core.client.stats.impl.IStat;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.description.Description;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public abstract class ClientStatFormatter extends StatFormatter {

    protected ClientStatFormatter(IClientStat iClientStat) {
        super(iClientStat);
    }

    @Override
    public Description getDescription(IStat stat, StatContainer container, String period) {
        IClientStat clientStat = (IClientStat) Objects.requireNonNull(getStat());
        final List<Component> lore = new ArrayList<>(Arrays.stream(clientStat.getDescription()).map(UtilMessage::deserialize).toList());
        lore.add(Component.empty());
        lore.add(UtilMessage.deserialize("<white>Value</white>: <green>%s</green>", clientStat.getStat(container, period)));
        final ItemView itemView = ItemView.builder()
                .displayName(Component.text(clientStat.getName()))
                .lore(lore)
                .frameLore(true)
                .material(clientStat.getMaterial())
                .customModelData(clientStat.getCustomModelData())
                .glow(clientStat.isGlowing())
                .build();
        return Description.builder()
                .icon(itemView)
                .build();
    }
}
