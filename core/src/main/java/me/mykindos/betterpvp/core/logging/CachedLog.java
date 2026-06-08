package me.mykindos.betterpvp.core.logging;

import lombok.Data;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.HashMap;

@Data
public class CachedLog {

    private final String message;
    private final String action;
    private final long timestamp;
    private final HashMap<String, String> context;

    public Component getRelativeTimeComponent() {
        return Translations.component("core.log.cached-log.1",
                Component.text(UtilTime.getTime((System.currentTimeMillis() - timestamp), 2), NamedTextColor.WHITE))
                .append(Component.text(" "));
    }

    public Component getAbsoluteTimeComponent() {
        return Component.text(UtilTime.getDateTime(timestamp), NamedTextColor.WHITE);
    }

}
