package me.mykindos.betterpvp.clans.world.camp.upgrade;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Value;
import me.mykindos.betterpvp.clans.world.camp.storage.CampChest;
import me.mykindos.betterpvp.clans.world.camp.storage.StorehouseChests;
import me.mykindos.betterpvp.clans.world.camp.structure.CampStructures;
import me.mykindos.betterpvp.clans.world.camp.structure.CampUpgrades;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.world.construction.ConstructionService;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Storehouse upgrade: searches every item chest in the camp, by a typed name or by an item the member holds, and
 * lists each chest holding a match with how many it holds.
 */
@BPvPListener
@Singleton
public class ItemFinder implements Listener {

    public static final String ID = "item_finder";

    private final CampUpgrades upgrades;
    @Getter(AccessLevel.PACKAGE)
    private final StorehouseChests chests;

    @Inject
    public ItemFinder(@NotNull CampUpgrades upgrades, @NotNull StorehouseChests chests) {
        this.upgrades = upgrades;
        this.chests = chests;
        upgrades.declare(CampStructures.STOREHOUSE, ID, 2);
        upgrades.page(ID, (player, camp, structure, previous) ->
                new ItemFinderMenu(this, player, camp, previous, null).show(player));
    }

    public boolean isActive(@NotNull SiteKey key) {
        return upgrades.has(key, CampStructures.STOREHOUSE, ID);
    }

    /** Every item chest in camp {@code key} holding something that passes {@code test}, with how many it holds. */
    public @NotNull List<Found> find(@NotNull Player player, @NotNull SiteKey key, @NotNull Predicate<ItemStack> test) {
        final Optional<ConstructionService.Worksite> here = chests.here(player, key);
        if (here.isEmpty() || !isActive(key)) {
            return List.of();
        }
        final World world = here.get().getWorld();
        final List<Found> found = new ArrayList<>();
        for (CampChest chest : chests.chests(here.get().getHolding(), world)) {
            final Optional<Inventory> inventory = chests.inventory(world, chest);
            if (inventory.isEmpty()) {
                continue;
            }
            int count = 0;
            for (ItemStack item : inventory.get().getContents()) {
                if (item != null && !item.getType().isAir() && test.test(item)) {
                    count += item.getAmount();
                }
            }
            if (count > 0) {
                found.add(new Found(chest, count));
            }
        }
        return found;
    }

    /** Items whose name, or whose material's name, contains {@code query}, ignoring case. */
    public static @NotNull Predicate<ItemStack> named(@NotNull String query) {
        return item -> matches(PlainTextComponentSerializer.plainText().serialize(item.effectiveName()), query)
                || matches(item.getType().name().replace('_', ' '), query);
    }

    static boolean matches(@NotNull String name, @NotNull String query) {
        final String wanted = query.trim().toLowerCase(Locale.ROOT);
        return !wanted.isEmpty() && name.toLowerCase(Locale.ROOT).contains(wanted);
    }

    /** One item chest and how many matching items it holds. */
    @Value
    public static class Found {
        CampChest chest;
        int count;
    }
}
