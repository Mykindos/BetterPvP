package me.mykindos.betterpvp.core.logging.type.formatted.item;

import me.mykindos.betterpvp.core.logging.type.UUIDLogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class ContainerStoreItemLog extends FormattedItemLog {
    /**
     * @param time
     * @param item
     * @param mainPlayerName
     * @param name
     * @param location
     */
    public ContainerStoreItemLog(long time, UUID item, @Nullable String mainPlayerName, @Nullable String name, @Nullable Location location) {
        super(time, UUIDLogType.ITEM_CONTAINER_STORE, item, mainPlayerName, null, name, location);
    }

    @Override
    public Component getComponent() {
        return getTimeComponent().append(getPlayer1()).appendSpace()
                .append(Component.text("stored", NamedTextColor.RED)).appendSpace()
                .append(getItem()).appendSpace()
                .append(Component.text("in")).appendSpace()
                .append(getName()).appendSpace()
                .append(getLocation());
    }
}
