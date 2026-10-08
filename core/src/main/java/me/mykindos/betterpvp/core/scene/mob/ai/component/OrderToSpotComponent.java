package me.mykindos.betterpvp.core.scene.mob.ai.component;

import me.mykindos.betterpvp.core.scene.mob.SceneMob;
import me.mykindos.betterpvp.core.scene.mob.ai.AIComponent;
import me.mykindos.betterpvp.core.scene.mob.ai.AIControl;
import org.bukkit.Location;

import java.util.EnumSet;
import java.util.function.LongSupplier;

/**
 * Sends the mob to a random point 1 to 3 blocks from a spot after {@link #orderTo(Location)}, rests there, then lets
 * the components below it decide again.
 */
public class OrderToSpotComponent implements AIComponent {

    public OrderToSpotComponent(SceneMob mob) {
        this(mob, System::currentTimeMillis);
    }

    OrderToSpotComponent(SceneMob mob, LongSupplier clock) {
        throw new UnsupportedOperationException("not implemented");
    }

    /** How long the mob rests at the spot, between {@code minMillis} and {@code maxMillis}. */
    public OrderToSpotComponent rest(long minMillis, long maxMillis) {
        throw new UnsupportedOperationException("not implemented");
    }

    /** Sends the mob to a point near {@code spot}. */
    public void orderTo(Location spot) {
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
