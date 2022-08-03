package me.mykindos.betterpvp.lunar.nethandler.client;

import me.mykindos.betterpvp.lunar.nethandler.ByteBufWrapper;
import me.mykindos.betterpvp.lunar.nethandler.LCPacket;
import me.mykindos.betterpvp.lunar.nethandler.shared.LCNetHandler;
import lombok.Getter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class LCPacketNametagsOverride extends LCPacket {

    @Getter private UUID player;
    @Getter private List<String> tags;

    public LCPacketNametagsOverride() {}

    public LCPacketNametagsOverride(UUID player, List<String> tags) {
        this.player = player;
        this.tags = tags;
    }

    @Override
    public void write(ByteBufWrapper buf) throws IOException {
        buf.writeUUID(this.player);
        buf.buf().writeBoolean(this.tags != null);

        if (this.tags != null) {
            buf.writeVarInt(tags.size());

            for (String tag : tags) {
                buf.writeString(tag);
            }
        }
    }

    @Override
    public void read(ByteBufWrapper buf) throws IOException {
        this.player = buf.readUUID();

        if (buf.buf().readBoolean()) {
            int tagsSize = buf.readVarInt();
            this.tags = new ArrayList<>(tagsSize);

            for (int i = 0; i < tagsSize; i++) {
                tags.add(buf.readString());
            }
        }
    }

    @Override
    public void process(LCNetHandler handler) {
        ((LCNetHandlerClient) handler).handleOverrideNametags(this);
    }

}