package me.mykindos.betterpvp.core.logging.type.formatted.item;

import me.mykindos.betterpvp.core.logging.type.UUIDLogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class ContainerBreakItemLog extends FormattedItemLog {
    /**
     * @param time
     * @param item
     * @param mainPlayerName
     * @param name
     * @param location
     */
    public ContainerBreakItemLog(long time, UUID item, @Nullable String mainPlayerName, @Nullable String name, @Nullable Location location) {
        super(time, UUIDLogType.ITEM_CONTAINER_BREAK, item, mainPlayerName, null, name, location);
    }

    @Override
    public Component getComponent() {
        return getTimeComponent().append(getPlayer1()).appendSpace()
                .append(Component.text("caused")).appendSpace()
                .append(getItem()).appendSpace()
                .append(Component.text("to be dropped from block", NamedTextColor.DARK_RED)).appendSpace()
                .append(getName()).appendSpace()
                .append(getLocation());
    }
}
