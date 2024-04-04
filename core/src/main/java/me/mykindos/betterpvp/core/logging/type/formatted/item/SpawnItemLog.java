package me.mykindos.betterpvp.core.logging.type.formatted.item;

import me.mykindos.betterpvp.core.logging.type.UUIDLogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class SpawnItemLog extends FormattedItemLog {
    /**
     * @param time
     * @param item
     * @param offlinePlayer1
     * @param offlinePlayer2
     */
    public SpawnItemLog(long time, UUID item, @Nullable OfflinePlayer offlinePlayer1, @Nullable OfflinePlayer offlinePlayer2) {
        super(time, UUIDLogType.ITEM_SPAWN, item, offlinePlayer1, offlinePlayer2, null, null);
    }

    @Override
    public Component getComponent() {
        return getTimeComponent().append(getPlayer1()).appendSpace()
                .append(Component.text("spawned", NamedTextColor.BLUE)).appendSpace()
                .append(Component.text("and")).appendSpace()
                .append(Component.text("gave", NamedTextColor.GREEN)).appendSpace()
                .append(getItem()).appendSpace()
                .append(Component.text("to")).appendSpace()
                .append(getPlayer2());
    }
}
