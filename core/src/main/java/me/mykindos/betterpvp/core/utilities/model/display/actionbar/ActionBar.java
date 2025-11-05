package me.mykindos.betterpvp.core.utilities.model.display.actionbar;

import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.utilities.model.data.PriorityData;
import me.mykindos.betterpvp.core.utilities.model.data.PriorityDataBlockingQueue;
import me.mykindos.betterpvp.core.utilities.model.display.DisplayObject;
import me.mykindos.betterpvp.core.utilities.model.display.IDisplayQueue;
import me.mykindos.betterpvp.core.utilities.model.display.component.TimedComponent;
import net.kyori.adventure.text.Component;
import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

@Slf4j
public class ActionBar implements IDisplayQueue<DisplayObject<Component>> {

    protected static final Component EMPTY = Component.empty();

    /**
     * These components are sent to the player for a set amount of seconds, in order of priority, and are removed after being shown.
     * <p>
     * These take priority over static components.
     * Higher priority components are shown first.
     */
    protected final PriorityDataBlockingQueue<DisplayObject<Component>> components = new PriorityDataBlockingQueue<>(20);

    // Use a lock to synchronize access to the components PriorityQueue
    protected final Object lock = new Object();

    @Override
    public void add(int priority, DisplayObject<Component> component) {
        synchronized (lock) {
            components.put(priority, component);
            if (component instanceof TimedComponent timed && !timed.isWaitToExpire()) {
                timed.startTime();
            }
        }
    }

    @Override
    public void remove(DisplayObject<Component> component) {
        synchronized (lock) {
            components.removeIf(pair -> pair.getRight().equals(component));
        }
    }

    @Override
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

    @Override
    public void show(Gamer gamer) {
        // Cleanup the action bar
        cleanUp();

        // The component to show
        Component component;
        synchronized (lock) {
            component = hasElementsQueued() ?  nextComponent(gamer) : EMPTY;
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

    protected Component nextComponent(Gamer gamer) {
        synchronized (lock) {
            if (components.isEmpty()) {
                return EMPTY;
            }

            Pair<PriorityData, DisplayObject<Component>> peekPair = components.peek();
            DisplayObject<Component> display = peekPair.getRight();
            Component advComponent = display.getProvider().apply(gamer);

            //peek component is not valid, try and find a valid one
            if (advComponent == null) {
                //components iterator/splititerator methods are not guaranteed any order, filter for one
                Pair<PriorityData, DisplayObject<Component>> pair = components.stream().filter(priorityDataDisplayComponentPair ->
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

    public void cleanUp() {
        synchronized (lock) {
            // Clean up dynamic components that have expired
            components.removeIf(pair -> pair.getRight().isInvalid());
        }
    }
}