package me.mykindos.betterpvp.core.inventory.gui.structure;

import me.mykindos.betterpvp.core.inventory.gui.AbstractPagedGui;
import me.mykindos.betterpvp.core.inventory.gui.AbstractScrollGui;
import me.mykindos.betterpvp.core.inventory.gui.AbstractTabGui;
import org.jetbrains.annotations.NotNull;

/**
 * Registry class for default markers
 */
public class Markers {

    private Markers() {}
    
    /**
     * The marker for horizontal content list slots in {@link AbstractPagedGui PagedGuis},
     * {@link AbstractScrollGui ScrollGuis} and {@link AbstractTabGui TabGuis}
     */
    public static final @NotNull Marker CONTENT_LIST_SLOT_HORIZONTAL = new Marker(true);
    
    /**
     * The marker for vertical content list slots in {@link AbstractPagedGui PagedGuis},
     * {@link AbstractScrollGui ScrollGuis} and {@link AbstractTabGui TabGuis}
     */
    public static final @NotNull Marker CONTENT_LIST_SLOT_VERTICAL = new Marker(false);
    
}
