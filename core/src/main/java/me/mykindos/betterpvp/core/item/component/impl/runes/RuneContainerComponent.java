package me.mykindos.betterpvp.core.item.component.impl.runes;

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
 * Defines the maximum number of rune sockets an item can have.
 */
@Getter
public class RuneContainerComponent implements ItemComponent, LoreComponent {

    private static final NamespacedKey COMPONENT_KEY = new NamespacedKey("core", "rune-container");
    
    private final int sockets;
    private final List<Rune> runes;
    
    /**
     * Creates a new rune container with the specified number of sockets
     * 
     * @param sockets Maximum number of runes this item can hold
     */
    public RuneContainerComponent(int sockets) {
        this(sockets, new ArrayList<>());
    }
    
    /**
     * Creates a new rune container with the specified sockets and runes
     * 
     * @param sockets Maximum number of runes this item can hold
     * @param runes List of runes currently applied to the item
     */
    public RuneContainerComponent(int sockets, List<Rune> runes) {
        this.sockets = sockets;
        this.runes = new ArrayList<>(runes);
    }
    
    /**
     * Gets an unmodifiable view of the runes in this container
     * 
     * @return List of runes
     */
    public List<Rune> getRunes() {
        return Collections.unmodifiableList(runes);
    }
    
    /**
     * Gets the number of available sockets in this container
     * 
     * @return Number of available sockets
     */
    public int getAvailablesockets() {
        return sockets - runes.size();
    }

    /**
     * Checks if this container has a specific rune
     * @param rune The rune to check for
     * @return true if the rune is present in this container
     */
    public boolean hasRune(@NotNull Rune rune) {
        return runes.contains(rune);
    }
    
    /**
     * Checks if this container has any available sockets
     * 
     * @return true if there are available sockets
     */
    public boolean hasAvailableSockets() {
        return runes.size() < sockets;
    }

    @Override
    public boolean isCompatibleWith(@NotNull Item item) {
        return runes.stream().allMatch(rune -> rune.canApply(item));
    }

    @Override
    public @NotNull NamespacedKey getNamespacedKey() {
        return COMPONENT_KEY;
    }

    @Override
    public @NotNull ItemComponent copy() {
        return new RuneContainerComponent(sockets, List.copyOf(runes));
    }

    @Override
    public List<Component> getLines(ItemInstance item) {
        Preconditions.checkState(runes.size() <= sockets, "Too many runes on item");

        final List<Component> loreLines = new ArrayList<>();

        for (Rune rune : runes) {
            loreLines.add(Component.text("[", NamedTextColor.GRAY)
                    .append(Component.text("✔", NamedTextColor.GREEN))
                    .append(Component.text("]", NamedTextColor.GRAY))
                    .appendSpace()
                    .append(Component.text(rune.getName(), NamedTextColor.GREEN, TextDecoration.UNDERLINED)));

            final Component description = miniMessage.deserialize("<gray>" + rune.getDescription());
            final List<Component> runeDescription = ComponentWrapper.wrapLine(description);
            loreLines.addAll(runeDescription);

            if (runes.indexOf(rune) < sockets - 1) {
                loreLines.add(Component.empty());
            }
        }

        int unused = sockets - runes.size();
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
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        RuneContainerComponent that = (RuneContainerComponent) o;
        return sockets == that.sockets && Objects.equals(runes, that.runes);
    }

    @Override
    public int hashCode() {
        // sum the hash codes of the sockets and runes
        int result = Integer.hashCode(sockets);
        result = 31 * result + (runes != null ? runes.stream().mapToInt(Rune::hashCode).sum() : 0);
        return result;
    }
}