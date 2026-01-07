package me.mykindos.betterpvp.core.npc.listener;

import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.protocol.player.UserProfile;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfo;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfo.PlayerData;
import me.mykindos.betterpvp.core.npc.NPCRegistry;
import me.mykindos.betterpvp.core.npc.model.HumanNPC;
import me.mykindos.betterpvp.core.npc.model.NPC;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.HumanEntity;

import java.util.Iterator;
import java.util.Objects;
import java.util.UUID;

public class PlayerListPacketController implements PacketListener {

    private final NPCRegistry registry;

    public PlayerListPacketController(NPCRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        final PacketTypeCommon type = event.getPacketType();

        if (type != PacketType.Play.Server.PLAYER_INFO) {
            return; // We only care about tab
        }

        final WrapperPlayServerPlayerInfo packet = new WrapperPlayServerPlayerInfo(event);
        final Iterator<PlayerData> iterator = packet.getPlayerDataList().iterator();
        while (iterator.hasNext()) {
            final PlayerData entry = iterator.next();

            final UserProfile profile = entry.getUserProfile();
            if (profile == null) {
                continue; // Nothing to change
            }

            final UUID id = profile.getUUID();
            final NPC found = this.registry.getNPC(id);
            if (!(found instanceof HumanNPC npc)) {
                continue; // We won't touch non-human NPCs
            }

            // Check what kind of visibility we have and change the entry
            final HumanEntity entity = npc.getEntity();
            final String name = entity.getName();
            switch (npc.getPlayerListVisibility()) {
                case CUSTOM_NAME_VISIBLE -> {
                    final Component display = Objects.requireNonNullElse(entity.customName(), Component.text(name));
                    entry.setDisplayName(display);
                }
                case PROFILE_NAME_VISIBLE -> {
                    entry.setDisplayName(Component.text(name));
                }
                case INVISIBLE -> {
                    iterator.remove(); // Remove the entry
                    continue;
                }
            }

            // Update style to match the old one, if any
            final Component displayName = entry.getDisplayName();
            if (displayName != null) {
                entry.setDisplayName(entry.getDisplayName().applyFallbackStyle(displayName.style()));
            }
        }
    }
}
