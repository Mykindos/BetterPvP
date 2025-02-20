package me.mykindos.betterpvp.core.world.logger.formatters;

import me.mykindos.betterpvp.core.utilities.UtilItem;
import me.mykindos.betterpvp.core.world.logger.WorldLog;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;

public class BlockDropItemWorldLogFormatter implements WorldLogFormatter {


    @Override
    public String requiredAction() {
        return "BLOCK_DROP_ITEM";
    }

    @Override
    public Component getPrefix() {
        return Component.text(" - ", NamedTextColor.RED);
    }

    @Override
    public Component format(WorldLog log) {
        HashMap<String, String> metadata = log.getMetadata();

        ItemStack itemStack = log.getItemStack();
        if(itemStack == null) {
            itemStack = new ItemStack(Material.valueOf(log.getMaterial()));
        }

        String itemName = UtilItem.getDisplayNameAsString(itemStack);

        return Component.text(log.getMaterial().toLowerCase(), NamedTextColor.DARK_AQUA)
                .append(Component.text(" dropped ", NamedTextColor.GRAY))
                .append(Component.text(itemStack.getAmount() + "x " + itemName, NamedTextColor.DARK_AQUA).hoverEvent(itemStack));
    }
}
