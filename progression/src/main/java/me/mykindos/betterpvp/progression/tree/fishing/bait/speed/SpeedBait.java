package me.mykindos.betterpvp.progression.tree.fishing.bait.speed;

import me.mykindos.betterpvp.progression.tree.fishing.model.Bait;
import org.bukkit.entity.FishHook;

public class SpeedBait extends Bait {

    public SpeedBait(SpeedBaitType type) {
        super(type);
    }

    @Override
    protected void onTrack(FishHook hook) {
        final SpeedBaitType type = (SpeedBaitType) getType();
        hook.setWaitTime((int) (hook.getWaitTime() / type.getMultiplier()));
    }
}
