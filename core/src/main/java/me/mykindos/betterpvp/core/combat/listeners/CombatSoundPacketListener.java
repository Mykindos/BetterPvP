package me.mykindos.betterpvp.core.combat.listeners;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.Sound;
import org.bukkit.event.Listener;

@Singleton
@BPvPListener
public class CombatSoundPacketListener implements Listener {

    private final Core core;

    @Inject
    public CombatSoundPacketListener(Core core) {
        this.core = core;

        ProtocolLibrary.getProtocolManager().addPacketListener(
                new PacketAdapter(core, ListenerPriority.NORMAL, PacketType.Play.Server.NAMED_SOUND_EFFECT) {
                    @Override
                    public void onPacketSending(PacketEvent event) {
                        if (event.getPacketType() == PacketType.Play.Server.NAMED_SOUND_EFFECT) {
                            Sound sound = event.getPacket().getSoundEffects().read(0);
                            if (sound == Sound.ENTITY_PLAYER_ATTACK_SWEEP
                                    || sound == Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK
                                    || sound == Sound.ENTITY_PLAYER_ATTACK_NODAMAGE
                                    || sound == Sound.ENTITY_PLAYER_ATTACK_WEAK
                                    || sound == Sound.ENTITY_PLAYER_ATTACK_STRONG
                                    || sound == Sound.ENTITY_PLAYER_ATTACK_CRIT) {
                                {
                                    event.setCancelled(true);
                                }
                            }
                        }
                    };
                });
    }
}
