package me.mykindos.betterpvp.core.utilities.model.display;

import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.utilities.model.data.PriorityDataBlockingQueue;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.ref.WeakReference;
import java.util.UUID;

@Slf4j
public class TitleQueue {

    static final Component EMPTY = Component.empty();

    /**
     * These components are sent to the player for a set amount of seconds, in order of priority, and are removed after being shown.
     * <p>
     * These take priority over static components.
     * Higher priority components are shown first.
     */
    private final PriorityDataBlockingQueue<TitleComponent> components = new PriorityDataBlockingQueue<>(5);

    private WeakReference<TitleComponent> showing = new WeakReference<>(null);

    // Object used for synchronization
    private final Object lock = new Object();

    /**
     * Add a component to the title for a set amount of seconds.
     *
     * @param priority  The priority of the component. Lower numbers are shown first.
     * @param component The component to add.
     */
    public void add(int priority, TitleComponent component) {
        synchronized (lock) {
            components.put(priority, component);
            if (!component.isWaitToExpire()) {
                component.startTime();
            }
        }
    }

    public void remove(TitleComponent component) {
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
        synchronized (lock) {
            cleanUp();
            log.info(components.toString());
            // The component to show
            TitleComponent component = hasComponentsQueued() ? nextComponent(gamer) : null;
            if (component == null || (showing.get() == component && component.hasStarted())) {
                return; // Do not send a new title if the current one has already started and it's the same as this
            }

            // Send the action bar to the player
            final Player player = Bukkit.getPlayer(UUID.fromString(gamer.getUuid()));
            if (player != null) {
                log.info("Showing new");
                component.sendPlayer(player, gamer);
                showing = new WeakReference<>(component);
            }
        }
    }

    private TitleComponent nextComponent(Gamer gamer) {
        synchronized (lock) {
            if (components.isEmpty()) {
                return null;
            }

            TitleComponent display = components.peek().getValue();
            display.startTime();
            log.info(display.toString());
            return display;
            /*
            final Iterator<Pair<Integer, TitleComponent>> iterator = components.iterator();
            TitleComponent display;

            // Loop through the components until we find one that is not null
            // If we find one that is null, skip it and move on to the next one
            do {
                display = iterator.next().getRight();
                log.info("Next Component is " + display);
            } while (iterator.hasNext());

            // At this point, the `component` will not be null because we know that there is at least one element in the queue
            display.startTime();

            // But we don't know if its `advComponent` will be null
            return display;*/
        }
    }

    private void cleanUp() {
        synchronized (lock) {
            // Clean up dynamic components that have expired
            components.removeIf(pair -> pair.getRight().isInvalid());
        }
    }
}