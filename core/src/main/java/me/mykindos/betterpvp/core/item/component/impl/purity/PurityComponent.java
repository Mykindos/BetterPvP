package me.mykindos.betterpvp.core.item.component.impl.purity;

import me.mykindos.betterpvp.core.item.component.AbstractItemComponent;
import me.mykindos.betterpvp.core.item.component.ItemComponent;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Component that stores the purity level of an item.
 * Purity affects socket count and stat variance.
 * Items with purity must be attuned to reveal their purity information.
 */
public class PurityComponent extends AbstractItemComponent {

    private final ItemPurity purity;
    private final boolean attuned;

    /**
     * Creates a new PurityComponent with the specified purity level.
     * The item will not be attuned by default (purity hidden).
     *
     * @param purity The purity level
     */
    @Contract(pure = true)
    public PurityComponent(@NotNull ItemPurity purity) {
        this(purity, false);
    }

    /**
     * Creates a new PurityComponent with the specified purity level and attuned state.
     *
     * @param purity  The purity level
     * @param attuned Whether the item is attuned (purity revealed)
     */
    @Contract(pure = true)
    public PurityComponent(@NotNull ItemPurity purity, boolean attuned) {
        super("purity");
        this.purity = Objects.requireNonNull(purity, "Purity cannot be null");
        this.attuned = attuned;
    }

    /**
     * Gets the purity level of this component.
     *
     * @return The purity level
     */
    @NotNull
    public ItemPurity getPurity() {
        return purity;
    }

    /**
     * Checks if this item is attuned (purity revealed).
     *
     * @return true if attuned, false if hidden
     */
    public boolean isAttuned() {
        return attuned;
    }

    /**
     * Creates a new PurityComponent with the specified attuned state.
     * This follows the immutable pattern used throughout the component system.
     *
     * @param attuned The new attuned state
     * @return A new PurityComponent with the updated attuned state
     */
    @Contract(pure = true, value = "_ -> new")
    public @NotNull PurityComponent withAttuned(boolean attuned) {
        return new PurityComponent(this.purity, attuned);
    }

    @Override
    @Contract(pure = true, value = "-> new")
    public @NotNull ItemComponent copy() {
        return new PurityComponent(purity, attuned);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        PurityComponent that = (PurityComponent) o;
        return purity == that.purity && attuned == that.attuned;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + purity.hashCode();
        result = 31 * result + Boolean.hashCode(attuned);
        return result;
    }
}
