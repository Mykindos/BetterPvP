package me.mykindos.betterpvp.core.client.stats.formatter.client;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.stats.StatContainer;
import me.mykindos.betterpvp.core.client.stats.formatter.ClientStatFormatter;
import me.mykindos.betterpvp.core.client.stats.impl.ClientStat;
import me.mykindos.betterpvp.core.client.stats.impl.IClientStat;
import me.mykindos.betterpvp.core.client.stats.impl.IStat;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import me.mykindos.betterpvp.core.utilities.model.description.Description;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Singleton
public class TimePlayedStatFormatter extends ClientStatFormatter {
    public TimePlayedStatFormatter() {
        super(ClientStat.TIME_PLAYED);
    }

    @Override
    public Description getDescription(IStat stat, StatContainer container, String period) {
        IClientStat clientStat = (IClientStat) Objects.requireNonNull(getStat());
        Double value = clientStat.getStat(container, period);
        Duration duration = Duration.ofMillis(value.longValue());
        final List<Component> lore = new ArrayList<>(Arrays.stream(clientStat.getDescription()).map(UtilMessage::deserialize).toList());
        lore.add(Component.empty());
        lore.add(UtilMessage.deserialize("<white>Value</white>: <green>%s</green>", UtilTime.humanReadableFormat(duration)));
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
