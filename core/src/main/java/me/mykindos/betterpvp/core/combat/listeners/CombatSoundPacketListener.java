package me.mykindos.betterpvp.core.combat.listeners;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.Sound;
import org.bukkit.event.Listener;

import java.util.List;

@Singleton
@PluginAdapter("ProtocolLib")
@BPvPListener
public class CombatSoundPacketListener extends PacketAdapter implements Listener {

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
        super(core, ListenerPriority.NORMAL, PacketType.Play.Server.NAMED_SOUND_EFFECT);
        ProtocolLibrary.getProtocolManager().addPacketListener(this);
    }

    @Override
    public void onPacketSending(PacketEvent event) {
        if (event.getPacketType() != PacketType.Play.Server.NAMED_SOUND_EFFECT) {
            return;
        }

        Sound sound = event.getPacket().getSoundEffects().read(0);
        if (sound != null && blockedSounds.contains(sound)) {
            event.setCancelled(true);
        }
    }
}
