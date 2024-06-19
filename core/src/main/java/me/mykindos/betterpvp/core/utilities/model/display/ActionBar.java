package me.mykindos.betterpvp.core.utilities.model.display;

import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.utilities.model.data.PriorityData;
import me.mykindos.betterpvp.core.utilities.model.data.PriorityDataBlockingQueue;
import net.kyori.adventure.text.Component;
import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

@Slf4j
public class ActionBar {

    static final Component EMPTY = Component.empty();

    /**
     * These components are sent to the player for a set amount of seconds, in order of priority, and are removed after being shown.
     * <p>
     * These take priority over static components.
     * Higher priority components are shown first.
     */
    private final PriorityDataBlockingQueue<DisplayComponent> components = new PriorityDataBlockingQueue<>(5);

    // Use a lock to synchronize access to the components PriorityQueue
    private final Object lock = new Object();

    public void add(int priority, DisplayComponent component) {
        synchronized (lock) {
            components.put(priority, component);
            if (component instanceof TimedComponent timed && !timed.isWaitToExpire()) {
                timed.startTime();
            }
        }
    }

    public void remove(DisplayComponent component) {
        synchronized (lock) {
            components.removeIf(pair -> pair.getRight().equals(component));
        }
    }

    public void clear() {
        synchronized (lock) {
            components.clear();
        }
    }

    public boolean hasComponentsQueued() {
        synchronized (lock) {
            return !components.isEmpty();
        }
    }

    public void show(Gamer gamer) {
        // Cleanup the action bar
        cleanUp();

        // The component to show
        Component component;
        synchronized (lock) {
            component = hasComponentsQueued() ?  nextComponent(gamer) : EMPTY;
        }
        if (component == null) {
            component = EMPTY;
        }

        // Send the action bar to the player
        final Player player = Bukkit.getPlayer(UUID.fromString(gamer.getUuid()));
        if (player != null) {
            player.sendActionBar(component);
        }
    }

    private Component nextComponent(Gamer gamer) {
        synchronized (lock) {
            if (components.isEmpty()) {
                return EMPTY;
            }

            Pair<PriorityData, DisplayComponent> peekPair = components.peek();
            DisplayComponent display = peekPair.getRight();
            Component advComponent = display.getProvider().apply(gamer);

            //peek component is not valid, try and find a valid one
            if (advComponent == null) {
                //components iterator/splititerator methods are not guaranteed any order, filter for one
                Pair<PriorityData, DisplayComponent> pair = components.stream().filter(priorityDataDisplayComponentPair ->
                        priorityDataDisplayComponentPair.getRight().getProvider().apply(gamer) != null).findFirst().orElse(null);

                if (pair == null) {
                    //there is not a valid component, return null
                    return null;
                }

                //update display/advcomponent
                display = pair.getRight();
                advComponent = display.getProvider().apply(gamer);
            }

            // At this point, the `component` will not be null because we know that there is at least one element in the queue
            if (display instanceof TimedComponent timed) {
                timed.startTime();
            }

            return advComponent;
        }
    }

    private void cleanUp() {
        synchronized (lock) {
            // Clean up dynamic components that have expired
            components.removeIf(pair -> pair.getRight().isInvalid());
        }
    }
}