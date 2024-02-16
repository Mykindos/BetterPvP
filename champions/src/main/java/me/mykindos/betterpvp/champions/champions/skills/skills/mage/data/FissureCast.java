package me.mykindos.betterpvp.champions.champions.skills.skills.mage.data;

import lombok.Data;
import me.mykindos.betterpvp.champions.champions.skills.skills.mage.axe.Fissure;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Data
public class FissureCast {

    private final Fissure fissure;
    private final Player player;
    private final int level;
    private final int distance;

    private Set<UUID> entitiesHit = new HashSet<>();
    private FissurePath fissurePath;

    private long startTime = System.currentTimeMillis();
    private int index;
    private boolean isFinished;

    public void process() {
        if(fissurePath == null || index >= fissurePath.getFissureBlocks().size() || UtilTime.elapsed(startTime, 20000)) {
            isFinished = true;
            return;
        }

        FissureBlock block = fissurePath.getFissureBlocks().get(index);
        fissure.doBlockUpdate(this, block);

        index++;
    }

}
