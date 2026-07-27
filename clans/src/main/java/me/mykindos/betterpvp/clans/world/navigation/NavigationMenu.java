package me.mykindos.betterpvp.clans.world.navigation;

import com.google.common.base.Preconditions;
import me.mykindos.betterpvp.clans.world.Island;
import me.mykindos.betterpvp.core.inventory.gui.AbstractGui;
import me.mykindos.betterpvp.core.inventory.item.Item;
import me.mykindos.betterpvp.core.inventory.item.impl.SimpleItem;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class NavigationMenu extends AbstractGui implements Windowed {

    private final Collection<Island> islands;

    public NavigationMenu(List<Island> islands) {
        super(9, 5);
        Preconditions.checkArgument(!islands.isEmpty(), "No islands to navigate to!");
        Preconditions.checkArgument(islands.size() <= 3, "Too many islands to navigate to! Max 3 islands allowed.");
        this.islands = islands;

        // last island should be on the center last
        final ArrayList<Island> buffer = new ArrayList<>(islands);
        Island center = buffer.removeLast();
        setItem(4, 4, createIslandButton(center));

        boolean side = ThreadLocalRandom.current().nextBoolean();
        boolean skipped = false;
        int row = 0;
        for (Island island : buffer) {
            // 1 to 3 offset from the center
            int offset = ThreadLocalRandom.current().nextInt(2, 4);
            offset *= side ? 1 : -1;

            // We get to skip one row for randomness’s sake
            if (!skipped && Math.random() < 0.5) {
                skipped = true;
                continue;
            }

            // Set the item
            setItem(4 + offset, row, createIslandButton(island));

            // invert the next side
            side = !side;
        }
    }

    private Item createIslandButton(final Island island) {
        return new SimpleItem(ItemView.builder()
                .material(Material.GRASS_BLOCK)
                .displayName(Component.text(island.name()))
                .build());
    }

    public Collection<Island> getIslands() {
        return Collections.unmodifiableCollection(islands);
    }

    @Override
    public @NotNull Component getTitle() {
        // todo: setup background
        return Component.text("Navigator");
    }
}
