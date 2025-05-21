package me.mykindos.betterpvp.core.utilities.model.display.experience;

import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.utilities.model.data.PriorityData;
import me.mykindos.betterpvp.core.utilities.model.data.PriorityDataBlockingQueue;
import me.mykindos.betterpvp.core.utilities.model.display.GamerDisplayObject;
import me.mykindos.betterpvp.core.utilities.model.display.IDisplayQueue;
import me.mykindos.betterpvp.core.utilities.model.display.experience.data.ExperienceLevelData;
import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

@Slf4j
public class ExperienceLevel implements IDisplayQueue<GamerDisplayObject<ExperienceLevelData>> {

    private static final ExperienceLevelData EMPTY = new ExperienceLevelData(0);

    /**
     * These components are sent to the player for a set amount of seconds, in order of priority, and are removed after being shown.
     * <p>
     * These take priority over static components.
     * Higher priority components are shown first.
     */
    private final PriorityDataBlockingQueue<GamerDisplayObject<ExperienceLevelData>> components = new PriorityDataBlockingQueue<>(5);

    // Use a lock to synchronize access to the components PriorityQueue
    private final Object lock = new Object();

    public void add(int priority, GamerDisplayObject<ExperienceLevelData> element) {
        synchronized (lock) {
            components.put(priority, element);
        }
    }

    public void remove(GamerDisplayObject<ExperienceLevelData> element) {
        synchronized (lock) {
            components.removeIf(pair -> pair.getRight().equals(element));
        }
    }

    public void clear() {
        synchronized (lock) {
            components.clear();
        }
    }

    @Override
    public boolean hasElementsQueued() {
        synchronized (lock) {
            return !components.isEmpty();
        }
    }

    public void show(Gamer gamer) {
        // Cleanup the action bar
        cleanUp();

        // The component to show
        ExperienceLevelData experienceLevelData;
        synchronized (lock) {
            experienceLevelData = hasElementsQueued() ? nextElement(gamer) : EMPTY;
        }
        if (experienceLevelData == null) {
            experienceLevelData = EMPTY;
        }

        // Send the action bar to the player
        final Player player = Bukkit.getPlayer(UUID.fromString(gamer.getUuid()));
        if (player != null) {
            player.setLevel(experienceLevelData.getLevel());
        }
    }

    private ExperienceLevelData nextElement(Gamer gamer) {
        synchronized (lock) {
            if (components.isEmpty()) {
                return EMPTY;
            }

            Pair<PriorityData, GamerDisplayObject<ExperienceLevelData>> peekPair = components.peek();
            GamerDisplayObject<ExperienceLevelData> display = peekPair.getRight();
            ExperienceLevelData experienceLevelData = display.getProvider().apply(gamer);

            //peek component is not valid, try and find a valid one
            if (experienceLevelData == null) {
                //components iterator/splititerator methods are not guaranteed any order, filter for one
                Pair<PriorityData, GamerDisplayObject<ExperienceLevelData>> pair = components.stream().filter(priorityDataDisplayComponentPair ->
                        priorityDataDisplayComponentPair.getRight().getProvider().apply(gamer) != null).findFirst().orElse(null);

                if (pair == null) {
                    //there is not a valid component, return null
                    return null;
                }

                //update display/advcomponent
                display = pair.getRight();
                experienceLevelData = display.getProvider().apply(gamer);
            }

            return experienceLevelData;
        }
    }

    public void cleanUp() {
        synchronized (lock) {
            // Clean up dynamic components that have expired
            components.removeIf(pair -> pair.getRight().isInvalid());
        }
    }
}