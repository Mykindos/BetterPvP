package me.mykindos.betterpvp.core.world.logger.formatters;

import me.mykindos.betterpvp.core.utilities.UtilItem;
import me.mykindos.betterpvp.core.world.logger.WorldLog;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;

public class InventoryMoveWorldLogFormatter implements WorldLogFormatter {


    @Override
    public String requiredAction() {
        return "BLOCK_MOVE_ITEM";
    }

    @Override
    public Component getPrefix() {
        return Component.text(" + ", NamedTextColor.GREEN);
    }

    @Override
    public Component format(WorldLog log) {
        HashMap<String, String> metadata = log.getMetadata();

        String source = "Unknown";
        if (metadata.containsKey("Source")) {
            source = metadata.get("Source");
        }

        ItemStack itemStack = log.getItemStack();
        if(itemStack == null) {
            itemStack = new ItemStack(Material.valueOf(log.getMaterial()));
        }

        String item = UtilItem.getDisplayNameAsString(itemStack);

        return Component.text(source.toLowerCase(), NamedTextColor.DARK_AQUA)
                .append(Component.text(" added ", NamedTextColor.GRAY))
                .append(Component.text(itemStack.getAmount() + "x " + item, NamedTextColor.DARK_AQUA).hoverEvent(itemStack))
                .append(Component.text(" to ", NamedTextColor.GRAY))
                .append(Component.text(log.getMaterial().toLowerCase(), NamedTextColor.DARK_AQUA));
    }
}
