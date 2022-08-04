package me.mykindos.betterpvp.lunar.nethandler.server;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.mykindos.betterpvp.lunar.nethandler.ByteBufWrapper;
import me.mykindos.betterpvp.lunar.nethandler.LCPacket;
import me.mykindos.betterpvp.lunar.nethandler.client.LCNetHandlerClient;
import me.mykindos.betterpvp.lunar.nethandler.shared.LCNetHandler;

import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
public final class LCPacketVoiceChannelRemove extends LCPacket {

    @Getter private UUID uuid;

    @Override
    public void write(ByteBufWrapper b) {
        b.writeUUID(uuid);
    }

    @Override
    public void read(ByteBufWrapper b) {
        this.uuid = b.readUUID();
    }

    @Override
    public void process(LCNetHandler handler) {
        ((LCNetHandlerClient) handler).handleVoiceChannelDelete(this);
    }
}
