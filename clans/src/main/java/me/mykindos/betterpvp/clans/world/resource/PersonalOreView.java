package me.mykindos.betterpvp.clans.world.resource;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.netty.buffer.ByteBufHelper;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.protocol.world.chunk.BaseChunk;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockChange;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerChunkData;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerMultiBlockChange;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import me.mykindos.betterpvp.clans.world.resource.archetype.PersonalOreArchetype;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Applies a player's personal mine state to the block updates leaving the server for them, so what they see at a point
 * they have mined out is their own stage rather than the world's untouched ore.
 * <p>
 * This is the seam that view has to be enforced at. {@link PersonalOreArchetype} keeps the world block pristine and
 * diverges only per player, but every ordinary path in the server treats the world as the truth to re-assert - most
 * sharply on a break, where cancelling the {@code BlockBreakEvent} is itself what makes CraftBukkit and the break
 * service resend the intact ore, always after whatever the archetype managed to send first. Rewriting the packet is
 * the one place the answer sticks, and it covers every other source at once: a neighbouring block update, another
 * plugin setting a block, a chunk coming back into view.
 * <p>
 * Runs on the netty thread against state the archetype keeps in concurrent maps. Packets the viewer has no stake in -
 * which is nearly all of them - are only peeked at, never decoded, because reading a packet through a wrapper is what
 * marks it to be re-encoded on the way out.
 */
@PluginAdapter("packetevents")
@Singleton
public class PersonalOreView implements PacketListener {

    private final PersonalOreArchetype archetype;
    private final Map<String, Integer> stateIds = new ConcurrentHashMap<>();

    @Inject
    private PersonalOreView(@NotNull PersonalOreArchetype archetype) {
        this.archetype = archetype;
        PacketEvents.getAPI().getEventManager().registerListener(this, PacketListenerPriority.NORMAL);
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (!(event.getPlayer() instanceof Player viewer)) {
            return;
        }
        final PacketTypeCommon type = event.getPacketType();
        switch (type) {
            case PacketType.Play.Server.BLOCK_CHANGE -> this.onBlockChange(event, viewer);
            case PacketType.Play.Server.MULTI_BLOCK_CHANGE -> this.onMultiBlockChange(event, viewer);
            case PacketType.Play.Server.CHUNK_DATA -> this.onChunkData(event, viewer);
            default -> { }
        }
    }

    private void onBlockChange(PacketSendEvent event, Player viewer) {
        final Vector3i position = peek(event, PacketWrapper::readBlockPosition);
        final String stage = archetype.viewAt(viewer.getUniqueId(), viewer.getWorld().getName(),
                position.getX(), position.getY(), position.getZ());
        if (stage == null) {
            return;
        }
        final int state = stateId(stage);
        if (state != -1) {
            new WrapperPlayServerBlockChange(event).setBlockID(state);
        }
    }

    private void onMultiBlockChange(PacketSendEvent event, Player viewer) {
        // The section position, of which only the chunk column matters here.
        final long encoded = peek(event, PacketWrapper::readLong);
        final int chunkX = (int) (encoded >> 42);
        final int chunkZ = (int) (encoded << 22 >> 42);

        final UUID viewerId = viewer.getUniqueId();
        final String world = viewer.getWorld().getName();
        if (archetype.viewInChunk(viewerId, world, chunkX, chunkZ).isEmpty()) {
            return;
        }
        final WrapperPlayServerMultiBlockChange packet = new WrapperPlayServerMultiBlockChange(event);
        for (WrapperPlayServerMultiBlockChange.EncodedBlock block : packet.getBlocks()) {
            final String stage = archetype.viewAt(viewerId, world, block.getX(), block.getY(), block.getZ());
            if (stage == null) {
                continue;
            }
            final int state = stateId(stage);
            if (state != -1) {
                block.setBlockId(state);
            }
        }
    }

    /**
     * Repaints the viewer's depleted points into the chunk itself. Without this a chunk coming back into view would
     * silently restock the mine, since the server builds it from the world - which still holds every ore intact.
     */
    private void onChunkData(PacketSendEvent event, Player viewer) {
        // Both chunk coordinates in one peek, packed so nothing is allocated per chunk packet.
        final long column = peek(event, wrapper -> ((long) wrapper.readInt() << 32) | (wrapper.readInt() & 0xFFFFFFFFL));
        final List<PersonalBlock> blocks = archetype.viewInChunk(viewer.getUniqueId(), viewer.getWorld().getName(),
                (int) (column >> 32), (int) column);
        if (blocks.isEmpty()) {
            return;
        }

        final BaseChunk[] sections = new WrapperPlayServerChunkData(event).getColumn().getChunks();
        final int minSection = viewer.getWorld().getMinHeight() >> 4;
        for (PersonalBlock block : blocks) {
            final int section = (block.getY() >> 4) - minSection;
            if (section < 0 || section >= sections.length || sections[section] == null) {
                continue;
            }
            final int state = stateId(block.getStage());
            if (state != -1) {
                sections[section].set(block.getX() & 15, block.getY() & 15, block.getZ() & 15, state);
            }
        }
    }

    /**
     * Reads just enough of the packet to decide whether it concerns this viewer, then rewinds the buffer. Going through
     * a real wrapper instead would hand the packet to PacketEvents to re-encode, which for chunk data means decoding
     * and rebuilding every section of every chunk the server sends to anyone.
     */
    private <T> T peek(PacketSendEvent event, Function<PacketWrapper<?>, T> reader) {
        final Object buffer = event.getByteBuf();
        final int index = ByteBufHelper.readerIndex(buffer);
        try {
            return reader.apply(new Peek(event));
        } finally {
            ByteBufHelper.readerIndex(buffer, index);
        }
    }

    /**
     * The protocol block state id for a degrade stage. Cached because the conversion goes through a block data string
     * and this is asked per block, per packet, on the netty thread. {@code -1} marks a stage that names nothing.
     */
    private int stateId(@NotNull String stage) {
        return stateIds.computeIfAbsent(stage, name -> {
            final Material material = Material.matchMaterial(name);
            if (material == null || !material.isBlock()) {
                return -1;
            }
            return SpigotConversionUtil.fromBukkitBlockData(material.createBlockData()).getGlobalId();
        });
    }

    /** A wrapper that reads nothing on construction, which is what keeps the packet off the re-encode path. */
    private static final class Peek extends PacketWrapper<Peek> {

        private Peek(PacketSendEvent event) {
            super(event, false);
        }
    }
}
