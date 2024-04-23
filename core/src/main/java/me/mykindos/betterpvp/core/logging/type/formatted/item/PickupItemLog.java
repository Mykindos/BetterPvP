package me.mykindos.betterpvp.core.logging.type.formatted.item;

import me.mykindos.betterpvp.core.logging.type.UUIDLogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;

import java.util.UUID;

public class PickupItemLog extends FormattedItemLog {
    public PickupItemLog(long time, UUID item, String mainPlayerName, Location location) {
        super(time, UUIDLogType.ITEM_PICKUP, item, mainPlayerName, null, null, location);
    }

    @Override
    public Component getComponent() {
        return getTimeComponent().append(getPlayer1()).appendSpace()
                        .append(Component.text("picked up", NamedTextColor.GREEN)).appendSpace()
                        .append(getItem()).appendSpace()
                        .append(getLocation());
    }
}
