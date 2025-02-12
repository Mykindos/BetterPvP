package me.mykindos.betterpvp.core.world.logger.formatters;

import me.mykindos.betterpvp.core.world.logger.WorldLog;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.HashMap;

public class BlockBreakWorldLogFormatter implements WorldLogFormatter {
    @Override
    public String requiredAction() {
        return "BLOCK_BREAK";
    }

    @Override
    public Component getPrefix() {
        return Component.text(" - ", NamedTextColor.RED);
    }

    @Override
    public Component format(WorldLog log) {
        HashMap<String, String> metadata = log.getMetadata();

        String player = "UNKNOWN";
        if(metadata.containsKey("PlayerName")) {
            player = metadata.get("PlayerName");
        }

        return Component.text(player, NamedTextColor.DARK_AQUA)
                .append(Component.text(" broke ", NamedTextColor.GRAY))
                .append(Component.text(log.getMaterial().toLowerCase(), NamedTextColor.DARK_AQUA));
    }
}
