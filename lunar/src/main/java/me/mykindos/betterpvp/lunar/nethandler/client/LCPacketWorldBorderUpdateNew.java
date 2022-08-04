package me.mykindos.betterpvp.lunar.nethandler.client;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.mykindos.betterpvp.lunar.nethandler.ByteBufWrapper;
import me.mykindos.betterpvp.lunar.nethandler.LCPacket;
import me.mykindos.betterpvp.lunar.nethandler.shared.LCNetHandler;

import java.io.IOException;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class LCPacketWorldBorderUpdateNew  extends LCPacket {

    private String id;

    private double minX, minZ, maxX, maxZ;
    private int durationTicks;

    private boolean cancelsEntry, cancelsExit;
    private int color;

    @Override
    public void write(ByteBufWrapper b) throws IOException {
        b.writeString(id);
        b.buf().writeDouble(minX);
        b.buf().writeDouble(minZ);
        b.buf().writeDouble(maxX);
        b.buf().writeDouble(maxZ);
        b.buf().writeInt(durationTicks);
        b.buf().writeBoolean(cancelsEntry);
        b.buf().writeBoolean(cancelsExit);
        b.buf().writeInt(color);
    }

    @Override
    public void read(ByteBufWrapper b) throws IOException {
        this.id = b.readString();
        this.minX = b.buf().readDouble();
        this.minZ = b.buf().readDouble();
        this.maxX = b.buf().readDouble();
        this.maxZ = b.buf().readDouble();
        this.durationTicks = b.buf().readInt();
        this.cancelsEntry = b.buf().readBoolean();
        this.cancelsExit = b.buf().readBoolean();
        this.color = b.buf().readInt();
    }

    @Override
    public void process(LCNetHandler handler) {
        ((LCNetHandlerClient) handler).handleWorldBorderUpdateNew(this);
    }
}
