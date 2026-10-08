package me.mykindos.betterpvp.core.scene.mob.ai.component;

import me.mykindos.betterpvp.core.scene.mob.SceneMob;
import me.mykindos.betterpvp.core.scene.mob.ai.AIComponent;
import me.mykindos.betterpvp.core.scene.mob.ai.AIControl;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.function.LongSupplier;

/**
 * Stops the mob, faces a player and holds IDLE for a set time after {@link #attend(Player)}.
 */
public class AttendComponent implements AIComponent {

    private final SceneMob mob;
    private final LongSupplier clock;

    private long durationMillis = 4_000L;
    @Nullable private Player player;
    private long until;

    public AttendComponent(SceneMob mob) {
        this(mob, System::currentTimeMillis);
    }

    public AttendComponent(SceneMob mob, LongSupplier clock) {
        this.mob = mob;
        this.clock = clock;
    }

    /** How long the mob attends to a player, in milliseconds. */
    public AttendComponent durationMillis(long durationMillis) {
        this.durationMillis = durationMillis;
        return this;
    }

    /** Attends to {@code player}, restarting the time if it is already attending. */
    public void attend(Player player) {
        this.player = player;
        this.until = clock.getAsLong() + durationMillis;
    }

    @Override
    public EnumSet<AIControl> getControls() {
        return EnumSet.of(AIControl.MOVE, AIControl.LOOK);
    }

    @Override
    public boolean canStart() {
        return player != null && clock.getAsLong() < until;
    }

    @Override
    public void start() {
        mob.stopMoving();
    }

    @Override
    public void tick() {
        final Mob bukkitMob = mob.getBukkitMob();
        if (player != null && bukkitMob != null && player.getWorld().equals(bukkitMob.getWorld())) {
            bukkitMob.lookAt(player.getEyeLocation());
        }
    }

    @Override
    public void stop() {
        player = null;
    }

}
