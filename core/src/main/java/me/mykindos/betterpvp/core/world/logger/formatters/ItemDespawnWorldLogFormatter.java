package me.mykindos.betterpvp.core.world.logger.formatters;

import me.mykindos.betterpvp.core.world.logger.WorldLog;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;

public class ItemDespawnWorldLogFormatter implements WorldLogFormatter {

    @Override
    public String requiredAction() {
        return "ITEM_DESPAWN";
    }

    @Override
    public Component getPrefix() {
        return Component.text(" - ", NamedTextColor.RED);
    }

    @Override
    public Component format(WorldLog log) {
        HashMap<String, String> metadata = log.getMetadata();

        String item = "Item";
        if (metadata.containsKey("ItemName")) {
            item = metadata.get("ItemName");
        }

        String amount = "1";
        if (metadata.containsKey("ItemAmount")) {
            amount = metadata.get("ItemAmount");
        }

        ItemStack itemStack = log.getItemStack();
        if(itemStack == null) {
            itemStack = new ItemStack(Material.valueOf(log.getMaterial()));
        }

        return Component.text(amount + "x " +  item, NamedTextColor.DARK_AQUA).hoverEvent(itemStack)
                .append(Component.text(" despawned ", NamedTextColor.GRAY));
    }
}
