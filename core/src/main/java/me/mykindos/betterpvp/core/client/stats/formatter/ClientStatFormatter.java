package me.mykindos.betterpvp.core.client.stats.formatter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import me.mykindos.betterpvp.core.client.stats.IClientStat;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.description.Description;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;

public abstract class ClientStatFormatter extends StatFormatter {
    private final IClientStat clientStat;

    public ClientStatFormatter(IClientStat iClientStat) {
        super(iClientStat.name());
        this.clientStat = iClientStat;
    }

    @Override
    public Description getDescription(String statName, Double stat) {
        final List<Component> lore = new ArrayList<>(Arrays.stream(clientStat.getDescription()).map(UtilMessage::deserialize).toList());
        lore.add(Component.empty());
        lore.add(UtilMessage.deserialize("<white>Value</white>: <green>%s</green>", stat));
        final ItemView itemView = ItemView.builder()
                .displayName(Component.text(clientStat.getName()))
                .lore(lore)
                .frameLore(true)
                .material(Material.BOOK)
                .build();
        return Description.builder()
                .icon(itemView)
                .build();
    }
}
