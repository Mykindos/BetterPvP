package me.mykindos.betterpvp.core.item.component.impl.socketables;

import com.google.common.base.Preconditions;
import lombok.Getter;
import me.mykindos.betterpvp.core.item.Item;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.component.ItemComponent;
import me.mykindos.betterpvp.core.item.component.LoreComponent;
import me.mykindos.betterpvp.core.utilities.ComponentWrapper;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static me.mykindos.betterpvp.core.utilities.UtilMessage.miniMessage;

/**
 * Container component that holds multiple runes for an item.
 * Defines the current available sockets and maximum upgradeable socket capacity.
 * <ul>
 *     <li>sockets: Current number of rune slots available</li>
 *     <li>maxSockets: Maximum number of sockets this item can be upgraded to</li>
 * </ul>
 */
@Getter
public class SocketableContainerComponent implements ItemComponent, LoreComponent {

    private static final NamespacedKey COMPONENT_KEY = new NamespacedKey("core", "rune-container");

    private final int sockets;
    private final int maxSockets;
    private final List<Socketable> socketables;
    
    /**
     * Creates a new rune container with no rune sockets.
     */
    public SocketableContainerComponent() {
        this(0, 0, new ArrayList<>());
    }

    /**
     * Creates a new rune container with the specified number of sockets.
     * For backwards compatibility, sets maxSockets equal to sockets.
     *
     * @param sockets Maximum number of runes this item can hold
     */
    public SocketableContainerComponent(int sockets) {
        this(sockets, sockets, new ArrayList<>());
    }

    /**
     * Creates a new rune container with the specified sockets and runes.
     * For backwards compatibility, sets maxSockets equal to sockets.
     *
     * @param sockets Maximum number of runes this item can hold
     * @param socketables List of runes currently applied to the item
     */
    public SocketableContainerComponent(int sockets, List<Socketable> socketables) {
        this(sockets, sockets, socketables);
    }

    /**
     * Creates a new rune container with specified sockets and max capacity.
     *
     * @param sockets Current number of available rune slots
     * @param maxSockets Maximum number of sockets this item can be upgraded to
     */
    public SocketableContainerComponent(int sockets, int maxSockets) {
        this(sockets, maxSockets, new ArrayList<>());
    }

    /**
     * Creates a new rune container with specified sockets, max capacity, and runes.
     *
     * @param sockets Current number of available rune slots
     * @param maxSockets Maximum number of sockets this item can be upgraded to
     * @param socketables List of runes currently applied to the item
     * @throws IllegalArgumentException if maxSockets is less than sockets
     */
    public SocketableContainerComponent(int sockets, int maxSockets, List<Socketable> socketables) {
        if (maxSockets < sockets) {
            throw new IllegalArgumentException(
                "maxSockets (" + maxSockets + ") cannot be less than sockets (" + sockets + ")"
            );
        }
        this.sockets = sockets;
        this.maxSockets = maxSockets;
        this.socketables = new ArrayList<>(socketables);
    }
    
    /**
     * Gets an unmodifiable view of the runes in this container
     * 
     * @return List of runes
     */
    public List<Socketable> getSocketables() {
        return Collections.unmodifiableList(socketables);
    }
    
    /**
     * Gets the number of available sockets in this container
     * 
     * @return Number of available sockets
     */
    public int getAvailablesockets() {
        return sockets - socketables.size();
    }

    /**
     * Checks if this container has a specific rune
     * @param socketable The rune to check for
     * @return true if the rune is present in this container
     */
    public boolean hasRune(@NotNull Socketable socketable) {
        return socketables.contains(socketable);
    }
    
    /**
     * Checks if this container has any available sockets
     * 
     * @return true if there are available sockets
     */
    public boolean hasAvailableSockets() {
        return socketables.size() < sockets;
    }

    @Override
    public boolean isCompatibleWith(@NotNull Item item) {
        return socketables.stream().allMatch(rune -> rune.canApply(item));
    }

    @Override
    public @NotNull NamespacedKey getNamespacedKey() {
        return COMPONENT_KEY;
    }

    @Override
    public @NotNull ItemComponent copy() {
        return new SocketableContainerComponent(sockets, maxSockets, List.copyOf(socketables));
    }

    @Override
    public List<Component> getLines(ItemInstance item) {
        Preconditions.checkState(socketables.size() <= sockets, "Too many runes on item");

        final List<Component> loreLines = new ArrayList<>();

        for (Socketable socketable : socketables) {
            loreLines.add(Component.text("[", NamedTextColor.GRAY)
                    .append(Component.text("✔", NamedTextColor.GREEN))
                    .append(Component.text("]", NamedTextColor.GRAY))
                    .appendSpace()
                    .append(Component.text(socketable.getName(), NamedTextColor.GREEN, TextDecoration.UNDERLINED)));

            final Component description = miniMessage.deserialize("<gray>" + socketable.getDescription());
            final List<Component> runeDescription = ComponentWrapper.wrapLine(description);
            loreLines.addAll(runeDescription);

            if (socketables.indexOf(socketable) < sockets - 1) {
                loreLines.add(Component.empty());
            }
        }

        int unused = sockets - socketables.size();
        for (int i = 0; i < unused; i++) {
            loreLines.add(Component.text("[", NamedTextColor.GRAY)
                    .append(Component.text("✘", NamedTextColor.RED))
                    .append(Component.text("]", NamedTextColor.GRAY))
                    .appendSpace()
                    .append(Component.text("Empty Rune Socket", NamedTextColor.GRAY)));
        }
        return loreLines;
    }

    @Override
    public int getRenderPriority() {
        return 2;
    }

    @Override
    public int getLorePage() {
        return 1;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        SocketableContainerComponent that = (SocketableContainerComponent) o;
        return sockets == that.sockets
            && maxSockets == that.maxSockets
            && Objects.equals(socketables, that.socketables);
    }

    @Override
    public int hashCode() {
        int result = Integer.hashCode(sockets);
        result = 31 * result + Integer.hashCode(maxSockets);
        result = 31 * result + (socketables != null ? socketables.stream().mapToInt(Socketable::hashCode).sum() : 0);
        return result;
    }
}