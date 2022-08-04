package me.mykindos.betterpvp.lunar.nethandler.client;

import lombok.Getter;
import me.mykindos.betterpvp.lunar.nethandler.ByteBufWrapper;
import me.mykindos.betterpvp.lunar.nethandler.LCPacket;
import me.mykindos.betterpvp.lunar.nethandler.shared.LCNetHandler;

import java.io.IOException;

public final class LCPacketServerUpdate extends LCPacket {

    @Getter private String server;

    public LCPacketServerUpdate() {}

    public LCPacketServerUpdate(String server) {
        this.server = server;
    }

    @Override
    public void write(ByteBufWrapper buf) throws IOException {
        buf.writeString(server);
    }

    @Override
    public void read(ByteBufWrapper buf) throws IOException {
        this.server = buf.readString();
    }

    @Override
    public void process(LCNetHandler handler) {
        ((LCNetHandlerClient) handler).handleServerUpdate(this);
    }

}