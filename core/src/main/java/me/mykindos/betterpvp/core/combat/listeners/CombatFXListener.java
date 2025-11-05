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
import me.mykindos.betterpvp.core.client.events.ClientJoinEvent;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.model.display.DisplayObject;
import me.mykindos.betterpvp.core.utilities.model.display.experience.data.ExperienceLevelData;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.List;

@Singleton
@PluginAdapter("packetevents")
@BPvPListener
public class CombatFXListener implements PacketListener, Listener {

    private final List<Sound> blockedSounds = List.of(
            Sound.ENTITY_PLAYER_ATTACK_SWEEP,
            Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK,
            Sound.ENTITY_PLAYER_ATTACK_NODAMAGE,
            Sound.ENTITY_PLAYER_ATTACK_WEAK,
            Sound.ENTITY_PLAYER_ATTACK_STRONG,
            Sound.ENTITY_PLAYER_ATTACK_CRIT
    );

    private final DisplayObject<ExperienceLevelData> levelDisplay;

    @Inject
    private CombatFXListener(Core core) {
        PacketEvents.getAPI().getEventManager().registerListener(this, PacketListenerPriority.NORMAL);
        this.levelDisplay = new DisplayObject<>((gamer) -> new ExperienceLevelData((int) gamer.getLastDealtDamageValue()));
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() != PacketType.Play.Server.SOUND_EFFECT) {
            return;
        }

        final WrapperPlayServerSoundEffect packet = new WrapperPlayServerSoundEffect(event);
        Sound sound = Registry.SOUNDS.get(packet.getSound().getSoundId().key());
        if (sound != null && blockedSounds.contains(sound)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onClientLogin(ClientJoinEvent event) {
        event.getClient().getGamer().getExperienceLevel().add(500, levelDisplay);
    }
}
