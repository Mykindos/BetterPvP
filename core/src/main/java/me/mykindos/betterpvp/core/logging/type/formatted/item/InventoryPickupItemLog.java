package me.mykindos.betterpvp.core.logging.type.formatted.item;

import me.mykindos.betterpvp.core.logging.type.UUIDLogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class InventoryPickupItemLog extends FormattedItemLog {
    /**
     * @param time
     * @param item
     * @param name
     * @param location
     */
    public InventoryPickupItemLog(long time, UUID item, @Nullable String name, @Nullable Location location) {
        super(time, UUIDLogType.ITEM_INVENTORY_PICKUP, item, null, null, name, location);
    }

    @Override
    public Component getComponent() {
        return getTimeComponent().append(getItem()).appendSpace()
                .append(Component.text("was")).appendSpace()
                .append(Component.text("picked up by block", NamedTextColor.DARK_GREEN)).appendSpace()
                .append(getName()).appendSpace()
                .append(getLocation());
    }
}
