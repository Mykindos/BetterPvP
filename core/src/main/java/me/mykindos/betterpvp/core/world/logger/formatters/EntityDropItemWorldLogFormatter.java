package me.mykindos.betterpvp.core.world.logger.formatters;

import me.mykindos.betterpvp.core.utilities.UtilItem;
import me.mykindos.betterpvp.core.world.logger.WorldLog;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;

public class EntityDropItemWorldLogFormatter implements WorldLogFormatter {

    @Override
    public String requiredAction() {
        return "ENTITY_DROP_ITEM";
    }

    @Override
    public Component getPrefix() {
        return Component.text(" - ", NamedTextColor.RED);
    }

    @Override
    public Component format(WorldLog log) {
        HashMap<String, String> metadata = log.getMetadata();

        String source = "UNKNOWN";
        if(metadata.containsKey("PlayerName")) {
            source = metadata.get("PlayerName");
        } else if(metadata.containsKey("EntityName")) {
            source = metadata.get("EntityName");
        }

        ItemStack itemStack = log.getItemStack();
        if(itemStack == null) {
            itemStack = new ItemStack(Material.valueOf(log.getMaterial()));
        }

        String item = UtilItem.getDisplayNameAsString(itemStack);

        return Component.text(source, NamedTextColor.DARK_AQUA)
                .append(Component.text(" dropped ", NamedTextColor.GRAY))
                .append(Component.text(itemStack.getAmount() + "x " +item, NamedTextColor.DARK_AQUA).hoverEvent(itemStack));
    }
}
