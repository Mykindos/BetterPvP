package me.mykindos.betterpvp.core.logging.type.formatted;

import lombok.Getter;
import me.mykindos.betterpvp.core.database.query.Statement;
import me.mykindos.betterpvp.core.logging.FormattedLog;
import me.mykindos.betterpvp.core.logging.type.UUIDLogType;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
public class FormattedItemLog extends FormattedLog {
    protected final List<Statement> locationStatements = new ArrayList<>();
    protected final UUIDLogType type;
    protected final UUID item;
    @Nullable
    protected final OfflinePlayer offlinePlayer1;
    @Nullable
    protected final OfflinePlayer offlinePlayer2;
    @Nullable
    protected final String name;
    @Nullable
    protected final Location location;

    /**
     *
     * @param time
     * @param type
     * @param item
     * @param offlinePlayer1
     * @param offlinePlayer2
     * @param name
     * @param location
     */
    public FormattedItemLog(long time, UUIDLogType type, UUID item, @Nullable OfflinePlayer offlinePlayer1, @Nullable OfflinePlayer offlinePlayer2, @Nullable String name, @Nullable Location location) {
        super(time);
        this.type = type;
        this.item = item;
        this.offlinePlayer1 = offlinePlayer1;
        this.offlinePlayer2 = offlinePlayer2;
        this.name = name;
        this.location = location;
    }

    @Override
    public Component getComponent() {
        return super.getComponent().append(
                UtilMessage.deserialize("<light_purple>%s</light_purple> <yellow>%s</yellow> <white>%s</white> <yellow>%s</yellow> at <aqua>%s</aqua> (<green>%s</green>, <green>%s</green>, <green>%s</green>) in <green>%s</green>",
                        item,
                        offlinePlayer1 == null ? null : offlinePlayer1.getName(),
                        type.name(),
                        offlinePlayer2 == null ? null : offlinePlayer2.getName(),
                        name,
                        location == null ? null : location.getBlockX(),
                        location == null ? null : location.getBlockY(),
                        location == null ? null : location.getBlockZ(),
                        location == null ? null : location.getWorld().getName()
                        ));
    }
}
