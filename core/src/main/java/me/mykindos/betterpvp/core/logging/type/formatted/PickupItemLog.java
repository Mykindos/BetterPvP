package me.mykindos.betterpvp.core.logging.type.formatted;

import me.mykindos.betterpvp.core.logging.type.UUIDLogType;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;

import java.util.UUID;

public class PickupItemLog extends FormattedItemLog {
    public PickupItemLog(long time, UUID item, OfflinePlayer offlinePlayer1, Location location) {
        super(time, UUIDLogType.ITEM_PICKUP, item, offlinePlayer1, null, null, location);
    }

    @Override
    public Component getComponent() {
        assert location != null;
        assert offlinePlayer1 != null;
        return getTimeComponent().append(
                UtilMessage.deserialize("<yellow>%s</yellow> <green>picked</green> up <light_purple>%s</light_purple> at (<green>%s</green>, <green>%s</green>, <green>%s</green>) in <green>%s</green>",
                        offlinePlayer1.getName(),
                        item,
                        location.getBlockX(),
                        location.getBlockY(),
                        location.getBlockZ(),
                        location.getWorld().getName()
                ));
    }
}
