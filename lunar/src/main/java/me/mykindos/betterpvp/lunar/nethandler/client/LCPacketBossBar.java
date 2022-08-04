package me.mykindos.betterpvp.lunar.nethandler.client;

import lombok.Getter;
import me.mykindos.betterpvp.lunar.nethandler.ByteBufWrapper;
import me.mykindos.betterpvp.lunar.nethandler.LCPacket;
import me.mykindos.betterpvp.lunar.nethandler.shared.LCNetHandler;

import java.io.IOException;

public final class LCPacketBossBar extends LCPacket {

    @Getter private int action;
    @Getter private String text;
    @Getter private float health;

    public LCPacketBossBar() {}

    public LCPacketBossBar(int action, String text, float health) {
        this.action = action;
        this.text = text;
        this.health = health;
    }

    @Override
    public void write(ByteBufWrapper buf) throws IOException {
        buf.writeVarInt(action);

        if (action == 0) {
            buf.writeString(text);
            buf.buf().writeFloat(health);
        }
    }

    @Override
    public void read(ByteBufWrapper buf) throws IOException {
        this.action = buf.readVarInt();

        if (action == 0) {
            text = buf.readString();
            health = buf.buf().readFloat();
        }
    }

    @Override
    public void process(LCNetHandler handler) {
        ((LCNetHandlerClient) handler).handleBossBar(this);
    }

}