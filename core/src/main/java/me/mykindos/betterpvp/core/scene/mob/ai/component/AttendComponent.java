package me.mykindos.betterpvp.core.scene.mob.ai.component;

import me.mykindos.betterpvp.core.scene.mob.SceneMob;
import me.mykindos.betterpvp.core.scene.mob.ai.AIComponent;
import me.mykindos.betterpvp.core.scene.mob.ai.AIControl;
import org.bukkit.entity.Player;

import java.util.EnumSet;
import java.util.function.LongSupplier;

/**
 * Stops the mob, faces a player and holds IDLE for a set time after {@link #attend(Player)}.
 */
public class AttendComponent implements AIComponent {

    public AttendComponent(SceneMob mob) {
        this(mob, System::currentTimeMillis);
    }

    AttendComponent(SceneMob mob, LongSupplier clock) {
        throw new UnsupportedOperationException("not implemented");
    }

    /** How long the mob attends to a player, in milliseconds. */
    public AttendComponent durationMillis(long durationMillis) {
        throw new UnsupportedOperationException("not implemented");
    }

    /** Attends to {@code player}, restarting the time if it is already attending. */
    public void attend(Player player) {
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
