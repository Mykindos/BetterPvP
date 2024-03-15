package me.mykindos.betterpvp.lunar.nethandler;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.lunar.nethandler.client.LCPacketBossBar;
import me.mykindos.betterpvp.lunar.nethandler.client.LCPacketClientVoice;
import me.mykindos.betterpvp.lunar.nethandler.client.LCPacketCooldown;
import me.mykindos.betterpvp.lunar.nethandler.client.LCPacketGhost;
import me.mykindos.betterpvp.lunar.nethandler.client.LCPacketHologram;
import me.mykindos.betterpvp.lunar.nethandler.client.LCPacketHologramRemove;
import me.mykindos.betterpvp.lunar.nethandler.client.LCPacketHologramUpdate;
import me.mykindos.betterpvp.lunar.nethandler.client.LCPacketModSettings;
import me.mykindos.betterpvp.lunar.nethandler.client.LCPacketNametagsOverride;
import me.mykindos.betterpvp.lunar.nethandler.client.LCPacketNametagsUpdate;
import me.mykindos.betterpvp.lunar.nethandler.client.LCPacketNotification;
import me.mykindos.betterpvp.lunar.nethandler.client.LCPacketServerRule;
import me.mykindos.betterpvp.lunar.nethandler.client.LCPacketServerUpdate;
import me.mykindos.betterpvp.lunar.nethandler.client.LCPacketStaffModState;
import me.mykindos.betterpvp.lunar.nethandler.client.LCPacketTeammates;
import me.mykindos.betterpvp.lunar.nethandler.client.LCPacketTitle;
import me.mykindos.betterpvp.lunar.nethandler.client.LCPacketUpdateWorld;
import me.mykindos.betterpvp.lunar.nethandler.client.LCPacketVoiceChannelSwitch;
import me.mykindos.betterpvp.lunar.nethandler.client.LCPacketVoiceMute;
import me.mykindos.betterpvp.lunar.nethandler.client.LCPacketWorldBorder;
import me.mykindos.betterpvp.lunar.nethandler.client.LCPacketWorldBorderCreateNew;
import me.mykindos.betterpvp.lunar.nethandler.client.LCPacketWorldBorderRemove;
import me.mykindos.betterpvp.lunar.nethandler.client.LCPacketWorldBorderUpdate;
import me.mykindos.betterpvp.lunar.nethandler.client.LCPacketWorldBorderUpdateNew;
import me.mykindos.betterpvp.lunar.nethandler.server.LCPacketVoice;
import me.mykindos.betterpvp.lunar.nethandler.server.LCPacketVoiceChannel;
import me.mykindos.betterpvp.lunar.nethandler.server.LCPacketVoiceChannelRemove;
import me.mykindos.betterpvp.lunar.nethandler.server.LCPacketVoiceChannelUpdate;
import me.mykindos.betterpvp.lunar.nethandler.shared.LCNetHandler;
import me.mykindos.betterpvp.lunar.nethandler.shared.LCPacketEmoteBroadcast;
import me.mykindos.betterpvp.lunar.nethandler.shared.LCPacketWaypointAdd;
import me.mykindos.betterpvp.lunar.nethandler.shared.LCPacketWaypointRemove;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public abstract class LCPacket {

    private static final Map<Class, Integer> classToId = new HashMap<>();
    private static final Map<Integer, Class> idToClass = new HashMap<>();

    static {
	// server
        addPacket(0, LCPacketClientVoice.class);
        addPacket(16, LCPacketVoice.class);
        addPacket(1, LCPacketVoiceChannelSwitch.class);
        addPacket(2, LCPacketVoiceMute.class);

        addPacket(17, LCPacketVoiceChannel.class);
        addPacket(18, LCPacketVoiceChannelRemove.class);
        addPacket(19, LCPacketVoiceChannelUpdate.class);

        // client
        addPacket(3, LCPacketCooldown.class);
        addPacket(4, LCPacketHologram.class);
        addPacket(6, LCPacketHologramRemove.class);
        addPacket(5, LCPacketHologramUpdate.class);
        addPacket(7, LCPacketNametagsOverride.class);
        addPacket(8, LCPacketNametagsUpdate.class);
        addPacket(9, LCPacketNotification.class);
        addPacket(10, LCPacketServerRule.class);
        addPacket(11, LCPacketServerUpdate.class);
        addPacket(12, LCPacketStaffModState.class);
        addPacket(13, LCPacketTeammates.class);
        addPacket(14, LCPacketTitle.class);
        addPacket(15, LCPacketUpdateWorld.class);
        addPacket(20, LCPacketWorldBorder.class);
        addPacket(21, LCPacketWorldBorderRemove.class);
        addPacket(22, LCPacketWorldBorderUpdate.class);
        addPacket(25, LCPacketGhost.class);
        addPacket(28, LCPacketBossBar.class);
        addPacket(29, LCPacketWorldBorderCreateNew.class);
        addPacket(30, LCPacketWorldBorderUpdateNew.class);
        addPacket(31, LCPacketModSettings.class);

        // shared
        addPacket(26, LCPacketEmoteBroadcast.class);
        addPacket(23, LCPacketWaypointAdd.class);
        addPacket(24, LCPacketWaypointRemove.class);
    }

    private Object attachment;

    public static LCPacket handle(byte[] data) {
        return handle(data, null);
    }

    public static LCPacket handle(byte[] data, Object attachment) {
        ByteBufWrapper wrappedBuffer = new ByteBufWrapper(Unpooled.wrappedBuffer(data));

        int packetId = wrappedBuffer.readVarInt();
        Class packetClass = idToClass.get(packetId);

        if (packetClass != null) {
            try {
                LCPacket packet = (LCPacket) packetClass.newInstance();

                packet.attach(attachment);
                packet.read(wrappedBuffer);

                return packet;
            } catch (IOException | InstantiationException | IllegalAccessException ex) {
                log.error("Failed to handle lunar packet", ex);
            }
        }

        return null;
    }

    public static byte[] getPacketData(LCPacket packet) {
        return getPacketBuf(packet).array();
    }

    public static ByteBuf getPacketBuf(LCPacket packet) {
        ByteBufWrapper wrappedBuffer = new ByteBufWrapper(Unpooled.buffer());
        wrappedBuffer.writeVarInt(classToId.get(packet.getClass()));

        try {
            packet.write(wrappedBuffer);
        } catch (IOException ex) {
            log.error("Failed to write lunar packet", ex);
        }

        return wrappedBuffer.buf();
    }

    private static void addPacket(int id, Class clazz) {
        if (classToId.containsKey(clazz)) {
            throw new IllegalArgumentException("Duplicate packet class (" + clazz.getSimpleName() + "), already used by " + classToId.get(clazz));
        } else if (idToClass.containsKey(id)) {
            throw new IllegalArgumentException("Duplicate packet ID (" + id + "), already used by " + idToClass.get(id).getSimpleName());
        }

        classToId.put(clazz, id);
        idToClass.put(id, clazz);
    }

    public abstract void write(ByteBufWrapper buf) throws IOException;

    public abstract void read(ByteBufWrapper buf) throws IOException;

    public abstract void process(LCNetHandler handler);

    public <T> void attach(T obj) {
        this.attachment = obj;
    }

    @SuppressWarnings("unchecked")
    public <T> T getAttachment() {
        return (T) attachment;
    }

    protected void writeBlob(ByteBufWrapper b, byte[] bytes) {
        b.buf().writeShort(bytes.length);
        b.buf().writeBytes(bytes);
    }

    protected byte[] readBlob(ByteBufWrapper b) {
        short key = b.buf().readShort();

        if (key < 0) {
            System.out.println("Key was smaller than nothing!  Weird key!");
        } else {
            byte[] blob = new byte[key];
            b.buf().readBytes(blob);
            return blob;
        }

        return null;
    }

}
