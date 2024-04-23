package me.mykindos.betterpvp.core.logging.type.formatted.item;

import me.mykindos.betterpvp.core.logging.type.UUIDLogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class LogoutItemLog extends FormattedItemLog {
    /**
     * @param time
     * @param item
     * @param mainPlayerName
     * @param location
     */
    public LogoutItemLog(long time, UUID item, @Nullable String mainPlayerName, @Nullable Location location) {
        super(time, UUIDLogType.ITEM_LOGOUT, item, mainPlayerName, null, null, location);
    }

    @Override
    public Component getComponent() {
        return getTimeComponent().append(getPlayer1()).appendSpace()
                .append(Component.text("logged out", NamedTextColor.BLUE)).appendSpace()
                .append(Component.text("with")).appendSpace()
                .append(getItem()).appendSpace()
                .append(getLocation());
    }
}
