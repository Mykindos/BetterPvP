package me.mykindos.betterpvp.core.logging.type.formatted.item;

import me.mykindos.betterpvp.core.logging.type.UUIDLogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class DespawnItemLog extends FormattedItemLog{
    /**
     * @param time
     * @param item
     * @param location
     */
    public DespawnItemLog(long time, UUID item, @Nullable Location location) {
        super(time, UUIDLogType.ITEM_DESPAWN, item, null, null, null, location);
    }

    @Override
    public Component getComponent() {
        return getTimeComponent().append(getItem()).appendSpace()
                .append(Component.text("despawned", NamedTextColor.RED).decoration(TextDecoration.BOLD, true)).appendSpace()
                .append(getLocation());
    }
}
