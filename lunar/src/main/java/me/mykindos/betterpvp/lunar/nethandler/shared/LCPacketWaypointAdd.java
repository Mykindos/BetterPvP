package me.mykindos.betterpvp.lunar.nethandler.shared;

import lombok.Getter;
import me.mykindos.betterpvp.lunar.nethandler.ByteBufWrapper;
import me.mykindos.betterpvp.lunar.nethandler.LCPacket;

import java.io.IOException;

public final class LCPacketWaypointAdd extends LCPacket {

    @Getter private String name;
    @Getter private String world;
    @Getter private int color;
    @Getter private int x;
    @Getter private int y;
    @Getter private int z;
    @Getter private boolean forced;
    @Getter private boolean visible;

    public LCPacketWaypointAdd() {}

    public LCPacketWaypointAdd(String name, String world, int color, int x, int y, int z, boolean forced, boolean visible) {
        this.name = name;
        this.world = world;
        this.color = color;
        this.x = x;
        this.y = y;
        this.z = z;
        this.forced = forced;
        this.visible = visible;
    }

    @Override
    public void write(ByteBufWrapper buf) throws IOException {
        buf.writeString(name);
        buf.writeString(world);
        buf.buf().writeInt(color);
        buf.buf().writeInt(x);
        buf.buf().writeInt(y);
        buf.buf().writeInt(z);
        buf.buf().writeBoolean(forced);
        buf.buf().writeBoolean(visible);
    }

    @Override
    public void read(ByteBufWrapper buf) throws IOException {
        this.name = buf.readString();
        this.world = buf.readString();
        this.color = buf.buf().readInt();
        this.x = buf.buf().readInt();
        this.y = buf.buf().readInt();
        this.z = buf.buf().readInt();
        this.forced = buf.buf().readBoolean();
        this.visible = buf.buf().readBoolean();
    }

    @Override
    public void process(LCNetHandler handler) {
        handler.handleAddWaypoint(this);
    }

}