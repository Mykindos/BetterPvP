package me.mykindos.betterpvp.core.logging.type.formatted.item;

import me.mykindos.betterpvp.core.logging.type.UUIDLogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class ContainerExplodeItemLog extends FormattedItemLog {
    /**
     * @param time
     * @param item
     * @param name
     * @param location
     */
    public ContainerExplodeItemLog(long time, UUID item, @Nullable String name, @Nullable Location location) {
        super(time, UUIDLogType.ITEM_CONTAINER_EXPLODE, item, null, null, name, location);
    }

    @Override
    public Component getComponent() {
        return getTimeComponent().append(getItem()).appendSpace()
                .append(Component.text("was")).appendSpace()
                .append(Component.text("dropped due to explosion", NamedTextColor.DARK_RED)).appendSpace()
                .append(Component.text("from")).appendSpace()
                .append(getName()).appendSpace()
                .append(getLocation());
    }
}
