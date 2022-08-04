package me.mykindos.betterpvp.lunar.nethandler.client;

import lombok.Getter;
import me.mykindos.betterpvp.lunar.nethandler.ByteBufWrapper;
import me.mykindos.betterpvp.lunar.nethandler.LCPacket;
import me.mykindos.betterpvp.lunar.nethandler.shared.LCNetHandler;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class LCPacketTeammates extends LCPacket {

    @Getter private UUID leader;
    @Getter private long lastMs;
    @Getter private Map<UUID, Map<String, Double>> players;

    public LCPacketTeammates() {}

    public LCPacketTeammates(UUID leader, long lastMs, Map<UUID, Map<String, Double>> players) {
        this.leader = leader;
        this.lastMs = lastMs;
        this.players = players;
    }

    @Override
    public void write(ByteBufWrapper buf) throws IOException {
        buf.buf().writeBoolean(leader != null);

        if (leader != null) {
            buf.writeUUID(leader);
        }

        buf.buf().writeLong(lastMs);
        buf.writeVarInt(this.players.size());

        this.players.forEach((uuid, posMap) -> {
            buf.writeUUID(uuid);
            buf.writeVarInt(posMap.size());

            posMap.forEach((key, val) -> {
                buf.writeString(key);
                buf.buf().writeDouble(val);
            });
        });
    }

    @Override
    public void read(ByteBufWrapper buf) throws IOException {
        if (buf.buf().readBoolean()) {
            this.leader = buf.readUUID();
        }

        this.lastMs = buf.buf().readLong();

        int playersSize = buf.readVarInt();
        this.players = new HashMap<>();

        for (int i = 0; i < playersSize; i++) {
            UUID uuid = buf.readUUID();
            int posMapSize = buf.readVarInt();
            Map<String, Double> posMap = new HashMap<>();

            for (int j = 0; j < posMapSize; j++) {
                String key = buf.readString();
                double val = buf.buf().readDouble();

                posMap.put(key, val);
            }

            this.players.put(uuid, posMap);
        }
    }

    @Override
    public void process(LCNetHandler handler) {
        ((LCNetHandlerClient) handler).handleTeammates(this);
    }

}