package me.mykindos.betterpvp.core.combat.listeners;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSoundEffect;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.Registry;
import org.bukkit.Sound;

import java.util.List;

@Singleton
@PluginAdapter("ProtocolLib")
@BPvPListener
public class CombatSoundPacketListener implements PacketListener {

    private final List<Sound> blockedSounds = List.of(
            Sound.ENTITY_PLAYER_ATTACK_SWEEP,
            Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK,
            Sound.ENTITY_PLAYER_ATTACK_NODAMAGE,
            Sound.ENTITY_PLAYER_ATTACK_WEAK,
            Sound.ENTITY_PLAYER_ATTACK_STRONG,
            Sound.ENTITY_PLAYER_ATTACK_CRIT
    );

    @Inject
    private CombatSoundPacketListener(Core core) {
        PacketEvents.getAPI().getEventManager().registerListener(this, PacketListenerPriority.NORMAL);
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() != PacketType.Play.Server.NAMED_SOUND_EFFECT) {
            return;
        }

        final WrapperPlayServerSoundEffect packet = new WrapperPlayServerSoundEffect(event);
        Sound sound = Registry.SOUNDS.get(packet.getSound().getSoundId().key());
        if (sound != null && blockedSounds.contains(sound)) {
            event.setCancelled(true);
        }
    }
}
