package me.mykindos.betterpvp.core.item.reforging;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

@BPvPListener
@Singleton
public class ReforgingListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    void onReforge(PlayerReforgeItemEvent event) {
        final Player player = event.getPlayer();

        // Normal cues
        new SoundEffect(Sound.ITEM_BRUSH_BRUSHING_GENERIC, 0f, 1.5f).play(player);
        new SoundEffect(Sound.ITEM_BRUSH_BRUSHING_SAND, 1f, 1.4f).play(player);
        new SoundEffect(Sound.BLOCK_AMETHYST_CLUSTER_HIT, 0.5f, 1.5f).play(player);
        new SoundEffect(Sound.BLOCK_BELL_RESONATE, 2f, 0.5f).play(player);
        new SoundEffect(Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 1.4f).play(player.getEyeLocation());
        new SoundEffect(Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1f, 1.5f).play(player.getEyeLocation());
        Particle.TRIAL_SPAWNER_DETECTION_OMINOUS.builder()
                .location(player.getLocation())
                .offset(0.5, 0.5, 0.5)
                .count(50)
                .receivers(60)
                .spawn();
        Particle.POOF.builder()
                .location(player.getLocation())
                .offset(0.5, 0.5, 0.5)
                .count(50)
                .extra(0.1)
                .receivers(60)
                .spawn();
    }

}
