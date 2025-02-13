package me.mykindos.betterpvp.core.world.logger.formatters;

import me.mykindos.betterpvp.core.utilities.UtilItem;
import me.mykindos.betterpvp.core.world.logger.WorldLog;
import me.mykindos.betterpvp.core.world.logger.WorldLogAction;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Objects;

public class ContainerDepositItemWorldLogFormatter implements WorldLogFormatter {


    @Override
    public String requiredAction() {
        return WorldLogAction.CONTAINER_DEPOSIT_ITEM.name();
    }

    @Override
    public Component getPrefix() {
        return Component.text(" + ", NamedTextColor.GREEN);
    }

    @Override
    public Component format(WorldLog log) {
        HashMap<String, String> metadata = log.getMetadata();

        String player = "UNKNOWN";
        if(metadata.containsKey("PlayerName")) {
            player = metadata.get("PlayerName");
        }

        ItemStack itemStack = log.getItemStack();
        if(itemStack == null) {
            itemStack = new ItemStack(Material.valueOf(log.getMaterial()));
        }

        String item = UtilItem.getDisplayNameAsString(itemStack);

        return Component.text(player, NamedTextColor.DARK_AQUA)
                .append(Component.text(" deposited ", NamedTextColor.GRAY))
                .append(Component.text(itemStack.getAmount() + "x " + item, NamedTextColor.DARK_AQUA).hoverEvent(itemStack))
                .append(Component.text(" into ", NamedTextColor.GRAY))
                .append(Component.text(log.getMaterial().toLowerCase(), NamedTextColor.DARK_AQUA));
    }
}
