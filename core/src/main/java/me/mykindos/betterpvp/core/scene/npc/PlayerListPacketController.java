package me.mykindos.betterpvp.core.scene.npc;

import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.protocol.player.UserProfile;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfo;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfo.PlayerData;
import me.mykindos.betterpvp.core.scene.SceneObjectRegistry;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.HumanEntity;

import java.util.Iterator;
import java.util.Objects;
import java.util.UUID;

public class PlayerListPacketController implements PacketListener {

    private final SceneObjectRegistry registry;

    public PlayerListPacketController(SceneObjectRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        final PacketTypeCommon type = event.getPacketType();

        if (type != PacketType.Play.Server.PLAYER_INFO) {
            return;
        }

        final WrapperPlayServerPlayerInfo packet = new WrapperPlayServerPlayerInfo(event);
        final Iterator<PlayerData> iterator = packet.getPlayerDataList().iterator();
        while (iterator.hasNext()) {
            final PlayerData entry = iterator.next();

            final UserProfile profile = entry.getUserProfile();
            if (profile == null) {
                continue;
            }

            final UUID id = profile.getUUID();
            final HumanNPC npc = registry.getObject(id, HumanNPC.class);
            if (npc == null) {
                continue;
            }

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
                    iterator.remove();
                    continue;
                }
            }

            final Component displayName = entry.getDisplayName();
            if (displayName != null) {
                entry.setDisplayName(entry.getDisplayName().applyFallbackStyle(displayName.style()));
            }
        }
    }
}
