package me.mykindos.betterpvp.core.utilities.model.display.experience;

import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.utilities.model.data.PriorityData;
import me.mykindos.betterpvp.core.utilities.model.data.PriorityDataBlockingQueue;
import me.mykindos.betterpvp.core.utilities.model.display.GamerDisplayObject;
import me.mykindos.betterpvp.core.utilities.model.display.IDisplayQueue;
import me.mykindos.betterpvp.core.utilities.model.display.experience.data.ExperienceBarData;
import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

@Slf4j
public class ExperienceBar implements IDisplayQueue<GamerDisplayObject<ExperienceBarData>> {

    private static final ExperienceBarData EMPTY = new ExperienceBarData(0);

    /**
     * These components are sent to the player for a set amount of seconds, in order of priority, and are removed after being shown.
     * <p>
     * These take priority over static components.
     * Higher priority components are shown first.
     */
    private final PriorityDataBlockingQueue<GamerDisplayObject<ExperienceBarData>> components = new PriorityDataBlockingQueue<>(5);

    // Use a lock to synchronize access to the components PriorityQueue
    private final Object lock = new Object();

    public void add(int priority, GamerDisplayObject<ExperienceBarData> component) {
        synchronized (lock) {
            components.put(priority, component);
        }
    }

    public void remove(GamerDisplayObject<ExperienceBarData> component) {
        synchronized (lock) {
            components.removeIf(pair -> pair.getRight().equals(component));
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
        ExperienceBarData experienceBarData;
        synchronized (lock) {
            experienceBarData = hasElementsQueued() ? nextElement(gamer) : EMPTY;
        }
        if (experienceBarData == null) {
            experienceBarData = EMPTY;
        }

        // Send the action bar to the player
        final Player player = Bukkit.getPlayer(UUID.fromString(gamer.getUuid()));
        if (player != null) {
            player.setExp(experienceBarData.getPercentage());
        }
    }

    private ExperienceBarData nextElement(Gamer gamer) {
        synchronized (lock) {
            if (components.isEmpty()) {
                return EMPTY;
            }

            Pair<PriorityData, GamerDisplayObject<ExperienceBarData>> peekPair = components.peek();
            GamerDisplayObject<ExperienceBarData> display = peekPair.getRight();
            ExperienceBarData experienceBarData = display.getProvider().apply(gamer);

            //peek component is not valid, try and find a valid one
            if (experienceBarData == null) {
                //components iterator/splititerator methods are not guaranteed any order, filter for one
                Pair<PriorityData, GamerDisplayObject<ExperienceBarData>> pair = components.stream().filter(priorityDataDisplayComponentPair ->
                        priorityDataDisplayComponentPair.getRight().getProvider().apply(gamer) != null).findFirst().orElse(null);

                if (pair == null) {
                    //there is not a valid component, return null
                    return null;
                }

                //update display/advcomponent
                display = pair.getRight();
                experienceBarData = display.getProvider().apply(gamer);
            }

            return experienceBarData;
        }
    }

    public void cleanUp() {
        synchronized (lock) {
            // Clean up dynamic components that have expired
            components.removeIf(pair -> pair.getRight().isInvalid());
        }
    }
}