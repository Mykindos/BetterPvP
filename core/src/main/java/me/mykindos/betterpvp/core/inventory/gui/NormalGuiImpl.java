package me.mykindos.betterpvp.core.inventory.gui;

import me.mykindos.betterpvp.core.inventory.gui.structure.Structure;
import org.jetbrains.annotations.NotNull;

/**
 * A normal {@link me.mykindos.betterpvp.core.inventory.gui.Gui} without any special features.
 * <p>
 * Use the static factory and builder functions, such as {@link me.mykindos.betterpvp.core.inventory.gui.Gui#normal()},
 * to get an instance of this class.
 */
final class NormalGuiImpl extends AbstractGui {
    
    /**
     * Creates a new {@link NormalGuiImpl}.
     *
     * @param width  The width of this Gui.
     * @param height The height of this Gui.
     */
    public NormalGuiImpl(int width, int height) {
        super(width, height);
    }
    
    /**
     * Creates a new {@link NormalGuiImpl}.
     *
     * @param structure The {@link Structure} to use.
     */
    public NormalGuiImpl(@NotNull Structure structure) {
        super(structure.getWidth(), structure.getHeight());
        applyStructure(structure);
    }
    
    public static class Builder extends AbstractBuilder<me.mykindos.betterpvp.core.inventory.gui.Gui, me.mykindos.betterpvp.core.inventory.gui.Gui.Builder.Normal> implements me.mykindos.betterpvp.core.inventory.gui.Gui.Builder.Normal {
        
        @Override
        public @NotNull Gui build() {
            if (structure == null)
                throw new IllegalStateException("Structure is not defined.");
            
            var gui = new NormalGuiImpl(structure);
            applyModifiers(gui);
            return gui;
        }
        
    }
    
}
