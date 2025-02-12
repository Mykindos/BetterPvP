package me.mykindos.betterpvp.core.world.logger.formatters;

import com.google.gson.Gson;
import me.mykindos.betterpvp.core.world.logger.WorldLog;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;

public class BlockExplodeWorldLogFormatter implements WorldLogFormatter {


    @Override
    public String requiredAction() {
        return "BLOCK_EXPLODE";
    }

    @Override
    public Component getPrefix() {
        return Component.text(" - ", NamedTextColor.RED);
    }

    @Override
    public Component format(WorldLog log) {
        HashMap<String, String> metadata = log.getMetadata();

        String source = "Unknown";
        if (metadata.containsKey("Source")) {
            source = metadata.get("Source");
        }


        return Component.text(source.toLowerCase(), NamedTextColor.DARK_AQUA)
                .append(Component.text(" destroyed ", NamedTextColor.GRAY))
                .append(Component.text(log.getMaterial().toLowerCase(), NamedTextColor.DARK_AQUA));
    }
}
