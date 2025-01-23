package me.mykindos.betterpvp.core.npc.controller;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.npc.NPCRegistry;
import me.mykindos.betterpvp.core.npc.model.HumanNPC;
import me.mykindos.betterpvp.core.npc.model.NPC;
import me.mykindos.betterpvp.core.packet.play.clientbound.WrapperPlayServerPlayerInfo;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.json.JSONComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.HumanEntity;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class PlayerListPacketController extends PacketAdapter {

    private final NPCRegistry registry;

    public PlayerListPacketController(NPCRegistry registry) {
        super(JavaPlugin.getPlugin(Core.class), PacketType.Play.Server.PLAYER_INFO);
        this.registry = registry;
    }

    @Override
    public void onPacketSending(PacketEvent event) {
        final PacketType type = event.getPacketType();

        if (type == PacketType.Play.Server.PLAYER_INFO) {
            final WrapperPlayServerPlayerInfo packet = new WrapperPlayServerPlayerInfo(event.getPacket());


            final List<PlayerInfoData> entries = new ArrayList<>();
            for (PlayerInfoData entry : packet.getEntries()) {
                final UUID id = entry.getProfileId();
                final NPC found = this.registry.getNPC(id);
                if (!(found instanceof HumanNPC npc)) {
                    entries.add(entry);
                    continue;
                }

                final WrappedGameProfile profile = entry.getProfile();
                final HumanEntity entity = npc.getEntity();
                final String name = entity.getName();
                switch (npc.getPlayerListVisibility()) {
                    case CUSTOM_NAME_VISIBLE -> {
                        final Component display = Objects.requireNonNullElse(entity.customName(), Component.text(name));
                        final String serialized = JSONComponentSerializer.json().serialize(display);
                        final WrappedChatComponent displayName = WrappedChatComponent.fromJson(serialized);
                        entries.add(new PlayerInfoData(id, 0, true, EnumWrappers.NativeGameMode.NOT_SET, profile, displayName));
                    }
                    case PROFILE_NAME_VISIBLE -> {
                        final String serialized = PlainTextComponentSerializer.plainText().serialize(Component.text(name));
                        final WrappedChatComponent displayName = WrappedChatComponent.fromText(serialized);
                        entries.add(new PlayerInfoData(id, 0, true, EnumWrappers.NativeGameMode.NOT_SET, profile, displayName));
                    }
                    case INVISIBLE -> {
                        final String serialized = PlainTextComponentSerializer.plainText().serialize(Component.text(name));
                        final WrappedChatComponent displayName = WrappedChatComponent.fromText(serialized);
                        entries.add(new PlayerInfoData(id, 0, false, EnumWrappers.NativeGameMode.NOT_SET, profile, displayName));
                    }
                }

                packet.setEntries(entries);
            }
        }
    }
}
