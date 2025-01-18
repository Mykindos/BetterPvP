package me.mykindos.betterpvp.core.logging.formatters;

import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.logging.CachedLog;
import me.mykindos.betterpvp.core.logging.repository.LogRepository;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.description.Description;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;

import java.util.HashMap;
import java.util.List;

public interface ILogFormatter {

    String getAction();

    Component formatLog(HashMap<String, String> context);

    default Description getDescription(CachedLog cachedLog, LogRepository logRepository, Windowed previous) {
        HashMap<String, String> context = cachedLog.getContext();
        List<Component> lore = new java.util.ArrayList<>(context.entrySet().stream().map(stringStringEntry -> {
            return UtilMessage.deserialize("<yellow>%s</yellow> - <green>%s</green>",
                    stringStringEntry.getKey(), stringStringEntry.getValue());
        }).toList());
        lore.add(0, cachedLog.getRelativeTimeComponent());

        ItemProvider itemProvider = ItemView.builder()
                .material(Material.DEBUG_STICK)
                .displayName(Component.text(cachedLog.getAction(), NamedTextColor.RED))
                .lore(lore)
                .build();
        return Description.builder()
                .icon(itemProvider)
                .build();
    }

}
