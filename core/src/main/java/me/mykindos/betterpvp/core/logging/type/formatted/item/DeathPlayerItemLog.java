package me.mykindos.betterpvp.core.logging.type.formatted.item;

import me.mykindos.betterpvp.core.logging.type.UUIDLogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class DeathPlayerItemLog extends FormattedItemLog{
    /**
     * @param time
     * @param item
     * @param mainPlayerName
     * @param otherPlayerName
     * @param location
     */
    public DeathPlayerItemLog(long time, UUID item, @Nullable String mainPlayerName, @Nullable String otherPlayerName, @Nullable Location location) {
        super(time, UUIDLogType.ITEM_DEATH_PLAYER, item, mainPlayerName, otherPlayerName, null, location);
    }

    @Override
    public Component getComponent() {
        return getTimeComponent().append(getPlayer1()).appendSpace()
                .append(Component.text("was")).appendSpace()
                .append(Component.text("killed", NamedTextColor.RED)).appendSpace()
                .append(Component.text("while holding")).appendSpace()
                .append(getItem()).appendSpace()
                .append(Component.text("by")).appendSpace()
                .append(getPlayer2()).appendSpace()
                .append(getLocation());
    }
}
