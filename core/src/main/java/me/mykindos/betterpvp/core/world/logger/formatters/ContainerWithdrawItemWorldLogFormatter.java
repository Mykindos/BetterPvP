package me.mykindos.betterpvp.core.world.logger.formatters;

import me.mykindos.betterpvp.core.world.logger.WorldLog;
import me.mykindos.betterpvp.core.world.logger.WorldLogAction;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Objects;

public class ContainerWithdrawItemWorldLogFormatter implements WorldLogFormatter {


    @Override
    public String requiredAction() {
        return WorldLogAction.CONTAINER_WITHDRAW_ITEM.name();
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

        ItemStack itemStack = log.getItemStack();
        if(itemStack == null) {
            itemStack = new ItemStack(Material.valueOf(log.getMaterial()));
        }

        Component itemName;
        if(itemStack.getItemMeta() != null) {
            ItemMeta meta = itemStack.getItemMeta();
            if(meta.displayName() != null) {
                itemName = meta.displayName();
            } else {
                itemName = Component.text(itemStack.getType().name().toLowerCase(), NamedTextColor.DARK_AQUA);
            }
        } else {
            itemName = Component.text(itemStack.getType().name().toLowerCase(), NamedTextColor.DARK_AQUA);
        }

        return Component.text(player, NamedTextColor.DARK_AQUA)
                .append(Component.text(" withdrawed " + itemStack.getAmount() + "x ", NamedTextColor.GRAY))
                .append(Objects.requireNonNull(itemName).hoverEvent(itemStack))
                .append(Component.text(" from ", NamedTextColor.GRAY))
                .append(Component.text(log.getMaterial().toLowerCase(), NamedTextColor.DARK_AQUA));
    }
}
