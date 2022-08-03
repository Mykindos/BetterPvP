package me.mykindos.betterpvp.lunar.nethandler.client;

import me.mykindos.betterpvp.lunar.nethandler.ByteBufWrapper;
import me.mykindos.betterpvp.lunar.nethandler.LCPacket;
import me.mykindos.betterpvp.lunar.nethandler.shared.LCNetHandler;
import lombok.Getter;

import java.io.IOException;

public final class LCPacketWorldBorder extends LCPacket {

    @Getter private String id;
    @Getter private String world;
    @Getter private boolean cancelsExit;
    @Getter private boolean canShrinkExpand;
    @Getter private int color = 0xFF3333FF;
    @Getter private double minX;
    @Getter private double minZ;
    @Getter private double maxX;
    @Getter private double maxZ;

    public LCPacketWorldBorder() {}

    public LCPacketWorldBorder(String id, String world, boolean cancelsExit, boolean canShrinkExpand, int color, double minX, double minZ, double maxX, double maxZ) {
        this.id = id;
        this.world = world;
        this.cancelsExit = cancelsExit;
        this.canShrinkExpand = canShrinkExpand;
        this.color = color;
        this.minX = minX;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxZ = maxZ;
    }

    @Override
    public void write(ByteBufWrapper buf) throws IOException {
        buf.buf().writeBoolean(id != null);

        if (id != null) {
            buf.writeString(id);
        }

        buf.writeString(world);
        buf.buf().writeBoolean(cancelsExit);
        buf.buf().writeBoolean(canShrinkExpand);
        buf.buf().writeInt(color);
        buf.buf().writeDouble(minX);
        buf.buf().writeDouble(minZ);
        buf.buf().writeDouble(maxX);
        buf.buf().writeDouble(maxZ);
    }

    @Override
    public void read(ByteBufWrapper buf) throws IOException {
        if (buf.buf().readBoolean()) {
            this.id = buf.readString();
        }

        this.world = buf.readString();
        this.cancelsExit = buf.buf().readBoolean();
        this.canShrinkExpand = buf.buf().readBoolean();
        this.color = buf.buf().readInt();
        this.minX = buf.buf().readDouble();
        this.minZ = buf.buf().readDouble();
        this.maxX = buf.buf().readDouble();
        this.maxZ = buf.buf().readDouble();
    }

    @Override
    public void process(LCNetHandler handler) {
        ((LCNetHandlerClient) handler).handleWorldBorder(this);
    }

}