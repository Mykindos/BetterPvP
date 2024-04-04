package me.mykindos.betterpvp.core.logging.type.formatted.item;

import me.mykindos.betterpvp.core.logging.type.UUIDLogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class DeathItemLog extends FormattedItemLog {
    /**
     * @param time
     * @param item
     * @param offlinePlayer1
     * @param location
     */
    public DeathItemLog(long time, UUID item, @Nullable OfflinePlayer offlinePlayer1, @Nullable Location location) {
        super(time, UUIDLogType.ITEM_DEATH, item, offlinePlayer1, null, null, location);
    }

    @Override
    public Component getComponent() {
        return getTimeComponent().append(getPlayer1()).appendSpace()
                .append(Component.text("died", NamedTextColor.RED)).appendSpace()
                .append(Component.text("with")).appendSpace()
                .append(getItem()).appendSpace()
                .append(getLocation());
    }
}
