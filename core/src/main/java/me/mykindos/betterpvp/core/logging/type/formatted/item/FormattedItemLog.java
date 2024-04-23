package me.mykindos.betterpvp.core.logging.type.formatted.item;

import lombok.Getter;
import me.mykindos.betterpvp.core.database.query.Statement;
import me.mykindos.betterpvp.core.logging.type.UUIDLogType;
import me.mykindos.betterpvp.core.logging.type.formatted.FormattedLog;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Getter
public class FormattedItemLog extends FormattedLog {
    protected final List<Statement> locationStatements = new ArrayList<>();
    protected final UUIDLogType type;
    protected final UUID item;
    @Nullable
    protected final String mainPlayerName;
    @Nullable
    protected final String otherPlayerName;
    @Nullable
    protected final String name;
    @Nullable
    protected final Location location;

    /**
     *
     * @param time
     * @param type
     * @param item
     * @param mainPlayerName
     * @param otherPlayerName
     * @param name
     * @param location
     */
    public FormattedItemLog(long time, UUIDLogType type, UUID item, @Nullable String mainPlayerName, @Nullable String otherPlayerName, @Nullable String name, @Nullable Location location) {
        super(time);
        this.type = type;
        this.item = item;
        this.mainPlayerName = mainPlayerName;
        this.otherPlayerName = otherPlayerName;
        this.name = name;
        this.location = location;
    }

    protected Component getLocation() {
        assert location != null;
        return UtilMessage.deserialize("at (<green>%s</green>, <green>%s</green>, <green>%s</green>) in <green>%s</green>"
                , location.getBlockX(), location.getBlockY(), location.getBlockZ(), location.getWorld().getName());
    }
    protected Component getItem() {
        return Component.text(String.valueOf(item), NamedTextColor.LIGHT_PURPLE);
    }
    protected Component getName() {
        assert name != null;
        return Component.text(name, NamedTextColor.AQUA);
    }
    protected Component getPlayer1() {
        assert mainPlayerName != null;
        return getPlayer(mainPlayerName);
    }
    protected Component getPlayer2() {
        assert otherPlayerName != null;
        return getPlayer(otherPlayerName);
    }
    protected Component getPlayer(String player) {
        return Component.text(Objects.requireNonNull(player), NamedTextColor.YELLOW);
    }

    @Override
    public Component getComponent() {
        return super.getComponent().append(
                UtilMessage.deserialize("<light_purple>%s</light_purple> <yellow>%s</yellow> <white>%s</white> <yellow>%s</yellow> at <aqua>%s</aqua> (<green>%s</green>, <green>%s</green>, <green>%s</green>) in <green>%s</green>",
                        item,
                        mainPlayerName,
                        type.name(),
                        otherPlayerName,
                        name,
                        location == null ? null : location.getBlockX(),
                        location == null ? null : location.getBlockY(),
                        location == null ? null : location.getBlockZ(),
                        location == null ? null : location.getWorld().getName()
                        ));
    }
}
