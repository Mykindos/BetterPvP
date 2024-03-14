package me.mykindos.betterpvp.core.framework.sidebar;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import lombok.Getter;
import lombok.NonNull;
import me.mykindos.betterpvp.core.framework.sidebar.protocol.ChannelInjector;
import me.mykindos.betterpvp.core.framework.sidebar.protocol.PacketIds;
import me.mykindos.betterpvp.core.framework.sidebar.text.TextProvider;
import me.mykindos.betterpvp.core.framework.sidebar.util.buffer.ByteBufNetOutput;
import me.mykindos.betterpvp.core.framework.sidebar.util.buffer.NetOutput;
import org.bukkit.entity.Player;

import static me.mykindos.betterpvp.core.framework.sidebar.SidebarLine.sendPacket;

/**
 * Encapsulates scoreboard objective
 *
 * @author CatCoder
 * @see <a href="https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/scoreboard/Objective.html">Bukkit
 * documentation</a>
 */
@Getter
public class ScoreboardObjective<R> {

    public static final int DISPLAY_SIDEBAR = 1;
    public static final int ADD_OBJECTIVE = 0;
    public static final int REMOVE_OBJECTIVE = 1;
    public static final int UPDATE_VALUE = 2;

    private final String name;
    private final TextProvider<R> textProvider;
    private R displayName;

    ScoreboardObjective(@NonNull String name, @NonNull R displayName, @NonNull TextProvider<R> textProvider) {
        Preconditions.checkArgument(
                name.length() <= 16, "Objective name exceeds 16 symbols limit");

        this.name = name;
        this.textProvider = textProvider;
        this.displayName = displayName;
    }

    void setDisplayName(@NonNull R displayName) {
        this.displayName = displayName;
    }

    void updateValue(@NonNull Player player) {
        ByteBuf packet = getPacket(player, UPDATE_VALUE);
        sendPacket(player, packet);
    }

    void create(@NonNull Player player) {
        ByteBuf packet = getPacket(player, ADD_OBJECTIVE);
        sendPacket(player, packet);
    }

    void remove(@NonNull Player player) {
        ByteBuf packet = getPacket(player, REMOVE_OBJECTIVE);
        sendPacket(player, packet);
    }

    void display(@NonNull Player player) {
        ByteBuf buf = ChannelInjector.IMP.getChannel(player).alloc().buffer();

        NetOutput output = new ByteBufNetOutput(buf);

        output.writeVarInt(PacketIds.OBJECTIVE_DISPLAY.getPacketId());

        output.writeByte(DISPLAY_SIDEBAR);
        output.writeString(name);

        sendPacket(player, buf);
    }

    private ByteBuf getPacket(@NonNull Player player, int mode) {

        ByteBuf buf = ChannelInjector.IMP.getChannel(player).alloc().buffer();

        NetOutput output = new ByteBufNetOutput(buf);

        output.writeVarInt(PacketIds.OBJECTIVE.getPacketId());

        output.writeString(name);
        output.writeByte(mode);

        if (mode == ADD_OBJECTIVE || mode == UPDATE_VALUE) {
            output.writeString(textProvider.asJsonMessage(player, displayName));
            output.writeVarInt(0); // Health display

        }


        return buf;
    }
}
