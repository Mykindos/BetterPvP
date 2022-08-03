package me.mykindos.betterpvp.lunar.nethandler.client;

import me.mykindos.betterpvp.lunar.nethandler.ByteBufWrapper;
import me.mykindos.betterpvp.lunar.nethandler.LCPacket;
import me.mykindos.betterpvp.lunar.nethandler.shared.LCNetHandler;
import lombok.Getter;

import java.io.IOException;

public final class LCPacketNotification extends LCPacket {

    @Getter private String message;
    @Getter private long durationMs;
    @Getter private String level;

    public LCPacketNotification() {}

    public LCPacketNotification(String message, long durationMs, String level) {
        this.message = message;
        this.durationMs = durationMs;
        this.level = level;
    }

    @Override
    public void write(ByteBufWrapper buf) throws IOException {
        buf.writeString(message);
        buf.buf().writeLong(durationMs);
        buf.writeString(level);
    }

    @Override
    public void read(ByteBufWrapper buf) throws IOException {
        this.message = buf.readString();
        this.durationMs = buf.buf().readLong();
        this.level = buf.readString();
    }

    @Override
    public void process(LCNetHandler handler) {
        ((LCNetHandlerClient) handler).handleNotification(this);
    }

}