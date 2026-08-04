package me.mykindos.betterpvp.core.scene.behavior;

import me.mykindos.betterpvp.core.scene.SceneObject;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import org.jetbrains.annotations.NotNull;

/**
 * Replays a sound at the object on a fixed beat - a creaking mast, a crackling brazier, a humming rune.
 * <p>
 * The sound is played positionally, so the client handles falloff and nobody out of earshot hears it. Wrap this in a
 * {@link ProximityGate} to also stop paying for the tick when nobody is close.
 */
public class AmbientSoundBehavior implements SceneBehavior {

    private final SceneObject owner;
    private final SoundEffect sound;
    private final int intervalTicks;

    private int sinceLast;

    /**
     * @param intervalTicks ticks between plays; a value below 1 is treated as every tick
     */
    public AmbientSoundBehavior(@NotNull SceneObject owner, @NotNull SoundEffect sound, int intervalTicks) {
        this.owner = owner;
        this.sound = sound;
        this.intervalTicks = Math.max(1, intervalTicks);
        this.sinceLast = this.intervalTicks;
    }

    @Override
    public void tick() {
        if (++sinceLast < intervalTicks || !owner.isMaterialized()) {
            return;
        }
        sinceLast = 0;
        sound.play(owner.getEntity().getLocation());
    }
}
