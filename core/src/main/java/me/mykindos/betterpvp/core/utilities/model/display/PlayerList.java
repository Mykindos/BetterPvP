package me.mykindos.betterpvp.core.utilities.model.display;

import me.mykindos.betterpvp.core.gamer.Gamer;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

public class PlayerList {

    static final Component EMPTY = Component.empty();

    private final List<DisplayComponent> headers = new ArrayList<>();
    private final List<DisplayComponent> footers = new ArrayList<>();

    // Use a lock to synchronize access to the components PriorityQueue
    private final Object lock = new Object();

    public List<DisplayComponent> getHeader() {
        synchronized (lock) {
            return headers;
        }
    }

    public List<DisplayComponent> getFooter() {
        synchronized (lock) {
            return footers;
        }
    }

    public void add(PlayerListType type, DisplayComponent component) {
        synchronized (lock) {
            List<DisplayComponent> components = type == PlayerListType.HEADER ? headers : footers;
            components.add(component);
            if (component instanceof TimedComponent timed && !timed.isWaitToExpire()) {
                timed.startTime();
            }
        }
    }

    public void remove(PlayerListType type, DisplayComponent component) {
        synchronized (lock) {
            List<DisplayComponent> components = type == PlayerListType.HEADER ? headers : footers;
            components.remove(component);
        }
    }

    public void clear() {
        synchronized (lock) {
            headers.clear();
            footers.clear();
        }
    }

    public void show(Gamer gamer) {
        // Cleanup the action bar
        cleanUp();

        // The component to show
        Component header;
        Component footer;
        synchronized (lock) {
            header = getComponent(PlayerListType.HEADER, gamer);
            footer = getComponent(PlayerListType.FOOTER, gamer);
        }

        // Send the action bar to the player
        final Player player = Bukkit.getPlayer(UUID.fromString(gamer.getUuid()));
        if (player != null) {
            player.sendPlayerListHeaderAndFooter(header, footer);
        }
    }

    private Component getComponent(PlayerListType type, Gamer gamer) {
        synchronized (lock) {
            List<DisplayComponent> components = type == PlayerListType.HEADER ? headers : footers;
            if (components.isEmpty()) {
                return EMPTY;
            }

            final Iterator<DisplayComponent> iterator = components.iterator();
            Component advComponent = null;

            do {
                DisplayComponent display = iterator.next();
                Component provided = display.getProvider().apply(gamer);
                if (provided != null) {
                    if (advComponent == null) {
                        advComponent = provided;
                    } else {
                        advComponent = advComponent.appendNewline().append(provided);
                    }

                    if (display instanceof TimedComponent timed) {
                        timed.startTime();
                    }
                }
            } while (iterator.hasNext());

            return advComponent;
        }
    }

    private void cleanUp() {
        synchronized (lock) {
            // Clean up dynamic components that have expired
            headers.removeIf(DisplayComponent::isInvalid);
            footers.removeIf(DisplayComponent::isInvalid);
        }
    }
}