package me.mykindos.betterpvp.core.utilities.model.display;

import me.mykindos.betterpvp.core.gamer.Gamer;
import net.kyori.adventure.text.Component;
import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.UUID;

public class ActionBar {

    static final Component EMPTY = Component.empty();

    /**
     * These components are sent to the player for a set amount of seconds, in order of priority, and are removed after being shown.
     * <p>
     * These take priority over static components.
     * Higher priority components are shown first.
     */
    private final PriorityQueue<Pair<Integer, DisplayComponent>> components = new PriorityQueue<>((o1, o2) -> Integer.compare(o1.getLeft(), o2.getLeft()) * -1);

    /**
     * Add a component to the action bar for a set amount of seconds.
     *
     * @param priority  The priority of the component. Lower numbers are shown first.
     * @param component The component to add.
     */
    public void add(int priority, DisplayComponent component) {
        components.add(Pair.of(priority, component));
        if (component instanceof TimedComponent timed && !timed.isWaitToExpire()) {
            timed.startTime();
        }
    }

    public void remove(DisplayComponent component) {
        components.removeIf(pair -> pair.getRight().equals(component));
    }

    public void clear() {
        components.clear();
    }

    public boolean hasComponentsQueued() {
        return !components.isEmpty();
    }

    public void show(Gamer gamer) {
        // Cleanup the action bar
        cleanUp();

        // The component to show
        Component component = hasComponentsQueued() ?  nextComponent(gamer) : EMPTY;

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
        if (components.isEmpty()) {
            return EMPTY;
        }

        final Iterator<Pair<Integer, DisplayComponent>> iterator = components.iterator();
        DisplayComponent display;
        Component advComponent;

        // Loop through the components until we find one that is not null
        // If we find one that is null, skip it and move on to the next one
        do {
            display = iterator.next().getRight();
            advComponent = display.getProvider().apply(gamer);
        } while (iterator.hasNext() && advComponent == null);

        // At this point, the `component` will not be null because we know that there is at least one element in the queue
        if (display instanceof TimedComponent timed) {
            timed.startTime();
        }

        // But we don't know if its `advComponent` will be null
        return advComponent;
    }

    private void cleanUp() {
        // Clean up dynamic components that have expired
        components.removeIf(pair -> pair.getRight().isInvalid());
    }

}
