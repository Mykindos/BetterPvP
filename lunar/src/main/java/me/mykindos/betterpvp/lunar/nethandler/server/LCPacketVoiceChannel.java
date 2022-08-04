package me.mykindos.betterpvp.lunar.nethandler.server;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.mykindos.betterpvp.lunar.nethandler.ByteBufWrapper;
import me.mykindos.betterpvp.lunar.nethandler.LCPacket;
import me.mykindos.betterpvp.lunar.nethandler.client.LCNetHandlerClient;
import me.mykindos.betterpvp.lunar.nethandler.shared.LCNetHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
public final class LCPacketVoiceChannel extends LCPacket {

    @Getter private UUID uuid;

    @Getter private String name;

    @Getter private Map<UUID, String> players;

    @Getter private Map<UUID, String> listening;

    @Override
    public void write(ByteBufWrapper b) {
        b.writeUUID(uuid);
        b.writeString(name);
        writeMap(b, players);
        writeMap(b, listening);
    }

    @Override
    public void read(ByteBufWrapper b) {
        this.uuid = b.readUUID();
        this.name = b.readString();
        this.players = readMap(b);
        this.listening = readMap(b);
    }

    private void writeMap(ByteBufWrapper b, Map<UUID, String> players) {
        b.writeVarInt(players.size());
        for (Map.Entry<UUID, String> player : players.entrySet()) {
            b.writeUUID(player.getKey());
            b.writeString(player.getValue());
        }
    }

    private Map<UUID, String> readMap(ByteBufWrapper b) {
        int size = b.readVarInt();

        Map<UUID, String> players = new HashMap<>();

        for (int i = 0; i < size; i++) {
            UUID uuid = b.readUUID();
            String name = b.readString();

            players.put(uuid, name);
        }

        return players;
    }

    @Override
    public void process(LCNetHandler handler) {
        ((LCNetHandlerClient) handler).handleVoiceChannels(this);
    }
}
