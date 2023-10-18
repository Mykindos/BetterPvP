package me.mykindos.betterpvp.lunar.nethandler.client;

import lombok.Getter;
import me.mykindos.betterpvp.lunar.nethandler.ByteBufWrapper;
import me.mykindos.betterpvp.lunar.nethandler.LCPacket;
import me.mykindos.betterpvp.lunar.nethandler.shared.LCNetHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class LCPacketNametagsUpdate extends LCPacket {

    @Getter private Map<UUID, List<String>> playersMap;

    public LCPacketNametagsUpdate() {}

    public LCPacketNametagsUpdate(Map<UUID, List<String>> playersMap) {
        this.playersMap = playersMap;
    }

    @Override
    public void write(ByteBufWrapper buf) throws IOException {
        buf.writeVarInt(playersMap == null ? -1 : playersMap.size());

        if (playersMap != null) {
            playersMap.forEach((uuid, tags) -> {
                buf.writeUUID(uuid);
                buf.writeVarInt(tags.size());

                for (String tag : tags) {
                    buf.writeString(tag);
                }
            });
        }
    }

    @Override
    public void read(ByteBufWrapper buf) throws IOException {
        int playersMapSize = buf.readVarInt();

        if (playersMapSize != -1) {
            this.playersMap = new HashMap<>();

            for (int i = 0; i < playersMapSize; i++) {
                UUID uuid = buf.readUUID();
                int tagsSize = buf.readVarInt();
                List<String> tags = new ArrayList<>(tagsSize);

                for (int j = 0; j < tagsSize; j++) {
                    tags.add(buf.readString());
                }

                this.playersMap.put(uuid, tags);
            }
        }
    }

    @Override
    public void process(LCNetHandler handler) {
        ((LCNetHandlerClient) handler).handleNametagsUpdate(this);
    }

}