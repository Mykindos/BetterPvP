package me.mykindos.betterpvp.core.item.attunement;

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
public class AttunementListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    void onAttunement(PlayerAttuneItemEvent event) {
        final Player player = event.getPlayer();

        // Normal cues
        new SoundEffect(Sound.ENTITY_SKELETON_DEATH, 2f, 0.5f).play(player);
        new SoundEffect(Sound.ITEM_SPYGLASS_USE, 0f, 1.4f).play(player);
        new SoundEffect(Sound.BLOCK_AMETHYST_CLUSTER_HIT, 1f, 1.5f).play(player);
        new SoundEffect(Sound.BLOCK_BELL_RESONATE, 2f, 0.5f).play(player);
        new SoundEffect(Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 1.4f).play(player.getEyeLocation());
        new SoundEffect(Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1f, 1.5f).play(player.getEyeLocation());
        Particle.ENCHANT.builder()
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

        // Highest tier cues
        switch (event.getPurityComponent().getPurity()) {
            case PRISTINE -> {
                new SoundEffect(Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 0.5f).play(player.getLocation());
                Particle.HAPPY_VILLAGER.builder()
                        .location(player.getLocation())
                        .offset(0.5, 0.5, 0.5)
                        .count(100)
                        .receivers(60)
                        .spawn();
            }
            case PERFECT -> {
                new SoundEffect(Sound.ENTITY_PLAYER_LEVELUP, 0f, 0.5f).play(player.getLocation());
                new SoundEffect(Sound.ENTITY_ENDER_DRAGON_GROWL, 1.5f, 0.5f).play(player.getLocation());
                new SoundEffect(Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.5f, 0.5f).play(player.getLocation());
                Particle.HAPPY_VILLAGER.builder()
                        .location(player.getLocation())
                        .offset(0.5, 0.5, 0.5)
                        .count(100)
                        .receivers(60)
                        .spawn();
            }
        }
    }

}
