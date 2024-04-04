package me.mykindos.betterpvp.core.logging.type.formatted.item;

import me.mykindos.betterpvp.core.logging.type.UUIDLogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class DropItemLog extends FormattedItemLog{
    /**
     * @param time
     * @param item
     * @param offlinePlayer1
     * @param location
     */
    public DropItemLog(long time, UUID item, @Nullable OfflinePlayer offlinePlayer1, @Nullable Location location) {
        super(time, UUIDLogType.ITEM_DROP, item, offlinePlayer1, null, null, location);
    }

    @Override
    public Component getComponent() {
        return getTimeComponent().append(getPlayer1()).appendSpace()
                .append(Component.text("dropped", NamedTextColor.RED)).appendSpace()
                .append(getItem()).appendSpace()
                .append(getLocation());
    }
}
