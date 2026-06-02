package me.mykindos.betterpvp.core.scene.mob.listener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.scene.SceneObjectRegistry;
import me.mykindos.betterpvp.core.scene.mob.SceneMob;
import me.mykindos.betterpvp.core.scene.mob.sound.MobSound;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityRemoveEvent;

/**
 * Tears a {@link SceneMob} out of the scene registry when its backing entity is removed from the
 * world for any reason (death, despawn, chunk unload, ...). Without this, a killed or unloaded mob
 * would linger forever in the registry - still ticked, still holding references to its target and
 * threat table - which is both a memory leak and a correctness bug.
 */
@BPvPListener
@Singleton
public class MobLifecycleListener implements Listener {

    private final SceneObjectRegistry registry;

    @Inject
    private MobLifecycleListener(SceneObjectRegistry registry) {
        this.registry = registry;
    }

    /**
     * Plays the mob's {@link MobSound#DEATH} cue on an actual death. Kept separate from
     * {@link #onRemove} because that fires for every removal cause (despawn, chunk unload) - a death
     * sound should only accompany a real death, which fires here first.
     */
    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        final SceneMob mob = registry.getObject(event.getEntity(), SceneMob.class);
        if (mob != null) {
            mob.getSounds().play(MobSound.DEATH);
            event.getDrops().clear();
            event.setDroppedExp(0);
        }
    }

    @EventHandler
    public void onRemove(EntityRemoveEvent event) {
        final SceneMob mob = registry.getObject(event.getEntity(), SceneMob.class);
        // isRegistered guards the re-entrant case: our own remove() unregisters first, then removes
        // the entity, which fires EntityRemoveEvent again.
        if (mob != null && mob.isRegistered()) {
            mob.remove();
        }
    }

}
