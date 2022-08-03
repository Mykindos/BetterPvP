package me.mykindos.betterpvp.lunar.nethandler.shared;

import me.mykindos.betterpvp.lunar.nethandler.ByteBufWrapper;
import me.mykindos.betterpvp.lunar.nethandler.LCPacket;
import lombok.Getter;

import java.io.IOException;
import java.util.UUID;

public final class LCPacketEmoteBroadcast extends LCPacket {

    @Getter private UUID uuid; // User doing the emote
    @Getter private int emoteId;

    public LCPacketEmoteBroadcast() {}

    public LCPacketEmoteBroadcast(UUID uuid, int emoteId) {
        this.uuid = uuid;
        this.emoteId = emoteId;
    }

    @Override
    public void write(ByteBufWrapper buf) throws IOException {
        buf.writeUUID(uuid);
        buf.buf().writeInt(emoteId);
    }

    @Override
    public void read(ByteBufWrapper buf) throws IOException {
        this.uuid = buf.readUUID();
        this.emoteId = buf.buf().readInt();
    }

    @Override
    public void process(LCNetHandler handler) {
        handler.handleEmote(this);
    }

}
