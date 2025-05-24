package me.mykindos.betterpvp.core.utilities.model.display;

import java.util.UUID;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.utilities.model.data.PriorityData;
import me.mykindos.betterpvp.core.utilities.model.data.PriorityDataBlockingQueue;
import me.mykindos.betterpvp.core.utilities.model.display.experience.data.ExperienceBarData;
import net.kyori.adventure.text.Component;
import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;


public abstract class AbstractDisplayQueue<T, G extends GamerDisplayObject<T>> implements IDisplayQueue<G> {

    private static final ExperienceBarData EMPTY = new ExperienceBarData(0);

    /**
     * These components are sent to the player for a set amount of seconds, in order of priority, and are removed after being shown.
     * <p>
     * These take priority over static components.
     * Higher priority components are shown first.
     */
    private final PriorityDataBlockingQueue<G> components = new PriorityDataBlockingQueue<>(5);

    // Use a lock to synchronize access to the components PriorityQueue
    private final Object lock = new Object();

    public void add(int priority, G element) {
        synchronized (lock) {
            components.put(priority, element);
        }
    }

    public void remove(G element) {
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
        T data;
        synchronized (lock) {
            data = hasElementsQueued() ? nextElement(gamer) : getEmpty();
        }
        if (data == null) {
            data = getEmpty();
        }

        // Send the action bar to the player
        final Player player = Bukkit.getPlayer(UUID.fromString(gamer.getUuid()));
        if (player != null) {
            sendTo(player, data);
        }
    }

    /**
     * Send this information to the player
     * @param player
     */
    public abstract void sendTo(Player player, T data);

    private T nextElement(Gamer gamer) {
        synchronized (lock) {
            if (components.isEmpty()) {
                return getEmpty();
            }

            Pair<PriorityData, G> peekPair = components.peek();
            G display = peekPair.getRight();
            T data = display.getProvider().apply(gamer);

            //peek component is not valid, try and find a valid one
            if (data == null) {
                //components iterator/splititerator methods are not guaranteed any order, filter for one
                Pair<PriorityData, G> pair = components.stream().filter(priorityDataDisplayComponentPair ->
                        priorityDataDisplayComponentPair.getRight().getProvider().apply(gamer) != null).findFirst().orElse(null);

                if (pair == null) {
                    //there is not a valid component, return null
                    return null;
                }

                //update display/advcomponent
                display = pair.getRight();
                data = display.getProvider().apply(gamer);
            }

            return data;
        }
    }

    public void cleanUp() {
        synchronized (lock) {
            // Clean up dynamic components that have expired
            components.removeIf(pair -> pair.getRight().isInvalid());
        }
    }

    /**
     * Get an element representing nothing, i.e. {@link Component#empty()}
     * @return
     */
    protected abstract T getEmpty();
}