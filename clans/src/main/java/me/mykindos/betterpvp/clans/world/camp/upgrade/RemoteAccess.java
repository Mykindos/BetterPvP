package me.mykindos.betterpvp.clans.world.camp.upgrade;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.AccessLevel;
import lombok.Getter;
import me.mykindos.betterpvp.clans.world.camp.CampPermissions;
import me.mykindos.betterpvp.clans.world.camp.storage.CampChest;
import me.mykindos.betterpvp.clans.world.camp.storage.StorehouseChests;
import me.mykindos.betterpvp.clans.world.camp.structure.CampStructures;
import me.mykindos.betterpvp.clans.world.camp.structure.CampUpgrades;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.world.construction.ConstructionService;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * Storehouse upgrade: open any of the camp's item chests from the Steward. The chest's own inventory opens, so what is
 * taken or put in is written back to its Storehouse when it closes, as if it had been opened by hand. Only those who
 * may open the camp's containers can use it.
 */
@BPvPListener
@Singleton
public class RemoteAccess implements Listener {

    public static final String ID = "remote_access";

    private final CampUpgrades upgrades;
    @Getter(AccessLevel.PACKAGE)
    private final StorehouseChests chests;
    private final CampPermissions permissions;

    @Inject
    public RemoteAccess(@NotNull CampUpgrades upgrades, @NotNull StorehouseChests chests,
                        @NotNull CampPermissions permissions) {
        this.upgrades = upgrades;
        this.chests = chests;
        this.permissions = permissions;
        upgrades.declare(CampStructures.STOREHOUSE, ID, 3);
        upgrades.page(ID, (player, camp, structure, previous) ->
                new RemoteAccessMenu(this, player, camp, previous).show(player));
    }

    public boolean isActive(@NotNull SiteKey key) {
        return upgrades.has(key, CampStructures.STOREHOUSE, ID);
    }

    /** Every item chest in camp {@code key}, empty when {@code player} is not there. */
    public @NotNull List<CampChest> chests(@NotNull Player player, @NotNull SiteKey key) {
        return chests.here(player, key)
                .map(worksite -> chests.chests(worksite.getHolding(), worksite.getWorld()))
                .orElse(List.of());
    }

    /**
     * Opens {@code chest} for {@code player}.
     *
     * @return the translation key of why it did not open, or null once open
     */
    public @Nullable String open(@NotNull Player player, @NotNull SiteKey key, @NotNull CampChest chest) {
        if (!isActive(key)) {
            return "clans.camp.upgrade.remote_access.inactive";
        }
        if (!permissions.mayOpenContainers(player, key.getOwnerId())) {
            return "clans.camp.upgrade.remote_access.not_allowed";
        }
        final Optional<ConstructionService.Worksite> here = chests.here(player, key);
        if (here.isEmpty()) {
            return "clans.camp.storage.not_here";
        }
        final Optional<Inventory> inventory = chests.inventory(here.get().getWorld(), chest);
        if (inventory.isEmpty()) {
            return "clans.camp.storage.out_of_reach";
        }
        player.openInventory(inventory.get());
        return null;
    }
}
