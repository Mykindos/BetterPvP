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
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Container component that holds multiple runes for an item.
 * Defines the maximum number of rune slots an item can have.
 */
@Getter
public class RuneContainerComponent implements ItemComponent, LoreComponent {

    private static final NamespacedKey COMPONENT_KEY = new NamespacedKey("champions", "rune-container");
    
    private final int slots;
    private final List<Rune> runes;
    
    /**
     * Creates a new rune container with the specified number of slots
     * 
     * @param slots Maximum number of runes this item can hold
     */
    public RuneContainerComponent(int slots) {
        this(slots, new ArrayList<>());
    }
    
    /**
     * Creates a new rune container with the specified slots and runes
     * 
     * @param slots Maximum number of runes this item can hold
     * @param runes List of runes currently applied to the item
     */
    public RuneContainerComponent(int slots, List<Rune> runes) {
        this.slots = slots;
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
     * Adds a rune to this container if there's space available
     * 
     * @param rune The rune to add
     * @return true if the rune was added, false if the container is full
     */
    public boolean addRune(Rune rune) {
        if (runes.size() >= slots) {
            return false;
        }
        
        return runes.add(rune);
    }
    
    /**
     * Removes a rune from this container
     * 
     * @param rune The rune to remove
     * @return true if the rune was removed
     */
    public boolean removeRune(Rune rune) {
        return runes.remove(rune);
    }
    
    /**
     * Gets the number of available slots in this container
     * 
     * @return Number of available slots
     */
    public int getAvailableSlots() {
        return slots - runes.size();
    }
    
    /**
     * Checks if this container has any available slots
     * 
     * @return true if there are available slots
     */
    public boolean hasAvailableSlots() {
        return runes.size() < slots;
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
        return new RuneContainerComponent(slots, List.copyOf(runes));
    }

    @Override
    public List<Component> getLines(ItemInstance item) {
        Preconditions.checkState(runes.size() <= slots, "Too many runes on item");

        final List<Component> loreLines = new ArrayList<>();

        for (Rune rune : runes) {
            loreLines.add(Component.text("[", NamedTextColor.GRAY)
                    .append(Component.text("✔", NamedTextColor.GREEN))
                    .append(Component.text("]", NamedTextColor.GRAY))
                    .appendSpace()
                    .append(Component.text("Rune: ", NamedTextColor.DARK_GREEN))
                    .appendSpace()
                    .append(Component.text(rune.getName() + " Rune", NamedTextColor.GREEN)));

            final Component description = Component.text(rune.getDescription(), NamedTextColor.GRAY);
            final List<Component> runeDescription = ComponentWrapper.wrapLine(description, 30);
            loreLines.addAll(runeDescription);

            if (runes.indexOf(rune) < runes.size() - 1) {
                loreLines.add(Component.empty());
            }
        }

        int unused = slots - runes.size();
        for (int i = 0; i < unused; i++) {
            loreLines.add(Component.text("[", NamedTextColor.GRAY)
                    .append(Component.text("✘", NamedTextColor.RED))
                    .append(Component.text("]", NamedTextColor.GRAY))
                    .appendSpace()
                    .append(Component.text("Unused Rune Slot", NamedTextColor.GRAY)));
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
        return slots == that.slots && Objects.equals(runes, that.runes);
    }

    @Override
    public int hashCode() {
        // sum the hash codes of the slots and runes
        int result = Integer.hashCode(slots);
        result = 31 * result + (runes != null ? runes.stream().mapToInt(Rune::hashCode).sum() : 0);
        return result;
    }
}