package me.mykindos.betterpvp.core.framework.sidebar.protocol;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import io.netty.buffer.ByteBuf;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import me.mykindos.betterpvp.core.framework.sidebar.text.TextProvider;
import me.mykindos.betterpvp.core.framework.sidebar.util.buffer.ByteBufNetOutput;
import me.mykindos.betterpvp.core.framework.sidebar.util.buffer.NetOutput;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

@UtilityClass
public class ScoreboardPackets {

    private final Splitter SPLITTER = Splitter.fixedLength(16);

    public final ChatColor[] COLORS = ChatColor.values();

    public final int TEAM_CREATED = 0;
    public final int TEAM_REMOVED = 1;
    public final int TEAM_UPDATED = 2;

    public <R> ByteBuf createTeamPacket(int mode, int index,
                                        @NonNull String teamName,
                                        @NonNull Player player,
                                        R text,
                                        @NonNull TextProvider<R> textProvider) {
        Preconditions.checkArgument(mode >= TEAM_CREATED && mode <= TEAM_UPDATED, "Invalid team mode");

        String teamEntry = COLORS[index].toString();

        ByteBuf buf = ChannelInjector.IMP.getChannel(player).alloc().buffer();

        NetOutput packet = new ByteBufNetOutput(buf);

        // construct the packet on lowest level for future compatibility

        packet.writeVarInt(PacketIds.UPDATE_TEAMS.getPacketId());

        packet.writeString(teamName);
        packet.writeByte(mode);

        if (mode == TEAM_REMOVED) {
            return buf;
        }

        packet.writeString("{\"text\":\"\"}"); // team display name


        writeDefaults(packet);
        packet.writeString(textProvider.asJsonMessage(player, text));
        packet.writeString("{\"text\":\"\"}");


        if (mode == TEAM_CREATED) {
            packet.writeVarInt(1); // number of players
            packet.writeString(teamEntry); // entries
        }

        return buf;
    }

    public ByteBuf createScorePacket(@NonNull Player player, int action, String objectiveName, int score, int index) {
        ByteBuf buf = ChannelInjector.IMP.getChannel(player).alloc().buffer();

        NetOutput output = new ByteBufNetOutput(buf);

        output.writeVarInt(PacketIds.UPDATE_SCORE.getPacketId());

        output.writeString(ScoreboardPackets.COLORS[index].toString());

        output.writeVarInt(action);

        output.writeString(objectiveName);

        if (action != 1) {
            output.writeVarInt(score);
        }

        return buf;
    }


    private static void writeDefaults(@NonNull NetOutput packet) {
        packet.writeByte(10); // friendly tags
        packet.writeString("always"); // name tag visibility
        packet.writeString("always"); // collision rule
        packet.writeVarInt(21);

    }

}
