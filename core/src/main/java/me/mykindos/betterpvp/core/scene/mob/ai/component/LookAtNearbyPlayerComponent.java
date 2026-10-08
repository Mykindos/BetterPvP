package me.mykindos.betterpvp.core.scene.mob.ai.component;

import me.mykindos.betterpvp.core.scene.mob.SceneMob;
import me.mykindos.betterpvp.core.scene.mob.ai.AIComponent;
import me.mykindos.betterpvp.core.scene.mob.ai.AIControl;

import java.util.EnumSet;

/**
 * Faces the nearest player within a radius while the mob isn't pathing, keeping to one player until they leave.
 */
public class LookAtNearbyPlayerComponent implements AIComponent {

    public LookAtNearbyPlayerComponent(SceneMob mob) {
        throw new UnsupportedOperationException("not implemented");
    }

    /** Distance, in blocks, within which a player is looked at. */
    public LookAtNearbyPlayerComponent radius(double radius) {
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
