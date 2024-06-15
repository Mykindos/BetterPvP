package me.mykindos.betterpvp.core.utilities.model.display;

import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.utilities.model.data.PriorityDataBlockingQueue;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

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

            DisplayComponent display = components.peek().getRight();
            Component advComponent = display.getProvider().apply(gamer);

            // At this point, the `component` will not be null because we know that there is at least one element in the queue
            if (display instanceof TimedComponent timed) {
                timed.startTime();
            }

            // But we don't know if its `advComponent` will be null
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