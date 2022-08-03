package me.mykindos.betterpvp.lunar.nethandler.client;

import me.mykindos.betterpvp.lunar.nethandler.ByteBufWrapper;
import me.mykindos.betterpvp.lunar.nethandler.LCPacket;
import me.mykindos.betterpvp.lunar.nethandler.shared.LCNetHandler;
import lombok.Getter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class LCPacketGhost extends LCPacket {

    @Getter private List<UUID> addGhostList;
    @Getter private List<UUID> removeGhostList;

    public LCPacketGhost() {}

    public LCPacketGhost(List<UUID> uuidList, List<UUID> removeGhostList) {
        this.addGhostList = uuidList;
        this.removeGhostList = removeGhostList;
    }

    @Override
    public void write(ByteBufWrapper buf) throws IOException {
        buf.writeVarInt(addGhostList.size());

        for (UUID uuid : addGhostList) {
            buf.writeUUID(uuid);
        }

        buf.writeVarInt(removeGhostList.size());

        for (UUID uuid : removeGhostList) {
            buf.writeUUID(uuid);
        }
    }

    @Override
    public void read(ByteBufWrapper buf) throws IOException {
        int addListSize = buf.readVarInt();
        this.addGhostList = new ArrayList<>(addListSize);

        for (int i = 0; i < addListSize; i++) {
            this.addGhostList.add(buf.readUUID());
        }

        int removeListSize = buf.readVarInt();
        this.removeGhostList = new ArrayList<>(removeListSize);

        for (int i = 0; i < removeListSize; i++) {
            this.removeGhostList.add(buf.readUUID());
        }
    }

    @Override
    public void process(LCNetHandler handler) {
        ((LCNetHandlerClient) handler).handleGhost(this);
    }

}