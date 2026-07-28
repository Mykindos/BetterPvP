package me.mykindos.betterpvp.core.item.impl.cannon.ride;

import com.google.common.base.Preconditions;
import me.mykindos.betterpvp.core.inventory.gui.AbstractGui;
import me.mykindos.betterpvp.core.inventory.item.Item;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.AbstractItem;
import me.mykindos.betterpvp.core.item.impl.cannon.model.CannonDestination;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

/**
 * Where a rider picks the landing site they will be fired at, shown as soon as they climb in. Nothing is lit until
 * they choose, and closing this menu without choosing puts them back on the ground.
 * <p>
 * Laid out like the navigator's island menu: one destination centred at the bottom and the rest scattered above it, so
 * the choices read as points on a map rather than a list.
 */
public class CannonDestinationMenu extends AbstractGui implements Windowed {

    private final Consumer<CannonDestination> onSelect;

    public CannonDestinationMenu(@NotNull List<CannonDestination> destinations,
                                 @NotNull Consumer<CannonDestination> onSelect) {
        super(9, 5);
        Preconditions.checkArgument(!destinations.isEmpty(), "No destinations to fire to!");
        this.onSelect = onSelect;

        final List<CannonDestination> buffer = new ArrayList<>(destinations);
        setItem(4, 4, createDestinationButton(buffer.removeLast()));

        boolean side = ThreadLocalRandom.current().nextBoolean();
        int row = 0;
        for (CannonDestination destination : buffer) {
            // 2 to 3 columns out from the centre, alternating sides so the layout stays legible as it grows
            final int offset = ThreadLocalRandom.current().nextInt(2, 4) * (side ? 1 : -1);
            setItem(Math.clamp(4 + offset, 0, 8), row, createDestinationButton(destination));
            side = !side;
            row = Math.min(row + 1, 3);
        }
    }

    private Item createDestinationButton(final CannonDestination destination) {
        return new AbstractItem() {
            @Override
            public ItemProvider getItemProvider() {
                return ItemView.builder()
                        .material(destination.getIcon())
                        .displayName(Component.text(destination.getName(), NamedTextColor.GREEN))
                        .build();
            }

            @Override
            public void handleClick(@NotNull ClickType clickType, @NotNull Player player,
                                    @NotNull InventoryClickEvent event) {
                // Selection before close: closing this menu is what cancels an unmade choice, so the choice has to
                // already be locked in by the time the close handler runs.
                onSelect.accept(destination);
                player.closeInventory();
            }
        };
    }

    @Override
    public @NotNull Component getTitle() {
        return Component.text("Select a Destination");
    }
}
