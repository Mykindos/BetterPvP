package me.mykindos.betterpvp.core.world.travel;

import com.google.common.base.Preconditions;
import me.mykindos.betterpvp.core.world.travel.Destination;
import me.mykindos.betterpvp.core.world.travel.TravelService;
import me.mykindos.betterpvp.core.inventory.gui.AbstractGui;
import me.mykindos.betterpvp.core.menu.Windowed;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class NavigationMenu extends AbstractGui implements Windowed {

    /** Returned by the slot pickers when the menu has no room left. */
    private static final int UNPLACED = -1;

    private final Collection<Destination> destinations;
    private final Set<Integer> occupiedSlots = new HashSet<>();

    public NavigationMenu(List<Destination> destinations, TravelService travelService) {
        this(destinations, travelService, false);
    }

    /**
     * @param immediate skip the departure hold. For a destination whose own journey is the wait — a ship's crossing —
     *                  standing still for three seconds first only delays it.
     */
    public NavigationMenu(List<Destination> destinations, TravelService travelService, boolean immediate) {
        super(9, 5);
        Preconditions.checkArgument(!destinations.isEmpty(), "No destinations to navigate to!");
        this.destinations = destinations;

        // last destination should be on the center last
        final ArrayList<Destination> buffer = new ArrayList<>(destinations);
        final Destination center = buffer.removeLast();
        placeButton(slotIndex(4, 4), center, travelService, immediate);

        boolean side = ThreadLocalRandom.current().nextBoolean();
        for (Destination destination : buffer) {
            final int scattered = scatterSlot(side);
            final int slot = scattered != UNPLACED ? scattered : nextOpenSlot();
            if (slot != UNPLACED) {
                placeButton(slot, destination, travelService, immediate);
            }

            side = !side;
        }
    }

    /**
     * Picks a slot around the center: 2-3 columns offset, on the given side, in any row above the center button.
     * Returns {@link #UNPLACED} if no unoccupied slot turned up within a handful of attempts, so the caller can fall
     * back to sequential filling rather than overwriting a button already placed there.
     */
    private int scatterSlot(boolean side) {
        for (int attempt = 0; attempt < 6; attempt++) {
            int offset = ThreadLocalRandom.current().nextInt(2, 4);
            offset *= side ? 1 : -1;
            final int column = 4 + offset;
            final int row = ThreadLocalRandom.current().nextInt(0, 4);
            if (column >= 0 && column < 9 && isOpen(slotIndex(column, row))) {
                return slotIndex(column, row);
            }
        }
        return UNPLACED;
    }

    private int nextOpenSlot() {
        for (int slot = 0; slot < 9 * 5; slot++) {
            if (isOpen(slot)) {
                return slot;
            }
        }
        return UNPLACED;
    }

    private boolean isOpen(int slot) {
        return !occupiedSlots.contains(slot);
    }

    private int slotIndex(int column, int row) {
        return row * 9 + column;
    }

    private void placeButton(int slot, Destination destination, TravelService travelService, boolean immediate) {
        setItem(slot, new DestinationButton(destination, travelService, immediate));
        occupiedSlots.add(slot);
    }

    public Collection<Destination> getDestinations() {
        return Collections.unmodifiableCollection(destinations);
    }

    @Override
    public @NotNull Component getTitle() {
        // todo: setup background
        return Component.text("Navigator");
    }
}
