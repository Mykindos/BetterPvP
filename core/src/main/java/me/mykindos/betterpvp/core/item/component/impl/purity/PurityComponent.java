package me.mykindos.betterpvp.core.item.component.impl.purity;

import me.mykindos.betterpvp.core.item.component.AbstractItemComponent;
import me.mykindos.betterpvp.core.item.component.ItemComponent;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Component that stores the purity level of an item.
 * Purity affects socket count and stat variance.
 */
public class PurityComponent extends AbstractItemComponent {

    private final ItemPurity purity;

    /**
     * Creates a new PurityComponent with the specified purity level.
     *
     * @param purity The purity level
     */
    @Contract(pure = true)
    public PurityComponent(@NotNull ItemPurity purity) {
        super("purity");
        this.purity = Objects.requireNonNull(purity, "Purity cannot be null");
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

    @Override
    @Contract(pure = true, value = "-> new")
    public @NotNull ItemComponent copy() {
        return new PurityComponent(purity);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        PurityComponent that = (PurityComponent) o;
        return purity == that.purity;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + purity.hashCode();
        return result;
    }
}
