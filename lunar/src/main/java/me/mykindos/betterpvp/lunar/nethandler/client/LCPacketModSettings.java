package me.mykindos.betterpvp.lunar.nethandler.client;

import me.mykindos.betterpvp.lunar.nethandler.ByteBufWrapper;
import me.mykindos.betterpvp.lunar.nethandler.LCPacket;
import me.mykindos.betterpvp.lunar.nethandler.client.obj.ModSettings;
import me.mykindos.betterpvp.lunar.nethandler.shared.LCNetHandler;

import java.io.IOException;

public final class LCPacketModSettings extends LCPacket {

    private ModSettings settings;

    public LCPacketModSettings() {}

    public LCPacketModSettings(ModSettings modSettings) {
        this.settings = modSettings;
    }


    @Override
    public void write(ByteBufWrapper buf) throws IOException {
        buf.writeString(ModSettings.GSON.toJson(this.settings));
    }

    @Override
    public void read(ByteBufWrapper buf) throws IOException {
        this.settings = ModSettings.GSON.fromJson(buf.readString(), ModSettings.class);
    }

    @Override
    public void process(LCNetHandler handler) {
        ((LCNetHandlerClient) handler).handleModSettings(this);
    }

    public ModSettings getSettings() {
        return settings;
    }
}
