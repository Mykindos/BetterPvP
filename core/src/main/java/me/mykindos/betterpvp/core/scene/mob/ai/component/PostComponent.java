package me.mykindos.betterpvp.core.scene.mob.ai.component;

import me.mykindos.betterpvp.core.scene.mob.SceneMob;
import me.mykindos.betterpvp.core.scene.mob.ai.AIComponent;
import me.mykindos.betterpvp.core.scene.mob.ai.AIControl;
import org.bukkit.Location;

import java.util.EnumSet;
import java.util.Optional;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * Walks the mob to its post and holds WORK there until it is replanned. The post is asked for only when the component
 * decides where to go. When the trip gives up, the mob rests and then asks again.
 */
public class PostComponent implements AIComponent {

    public PostComponent(SceneMob mob, Supplier<Optional<Location>> post) {
        this(mob, post, System::currentTimeMillis);
    }

    PostComponent(SceneMob mob, Supplier<Optional<Location>> post, LongSupplier clock) {
        throw new UnsupportedOperationException("not implemented");
    }

    /** Pathfinding speed multiplier used on the way to the post. */
    public PostComponent speed(double speed) {
        throw new UnsupportedOperationException("not implemented");
    }

    /** How long the mob rests after a trip gives up, between {@code minMillis} and {@code maxMillis}. */
    public PostComponent rest(long minMillis, long maxMillis) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public EnumSet<AIControl> getControls() {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public boolean canStart() {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public void tick() {
        throw new UnsupportedOperationException("not implemented");
    }

}
