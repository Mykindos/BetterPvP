package me.mykindos.betterpvp.clans.world.camp.upgrade;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.world.camp.storage.CampChest;
import me.mykindos.betterpvp.clans.world.camp.storage.SortingPlan;
import me.mykindos.betterpvp.clans.world.camp.storage.StorehouseChests;
import me.mykindos.betterpvp.clans.world.camp.structure.CampStructures;
import me.mykindos.betterpvp.clans.world.camp.structure.CampUpgrades;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.world.construction.ConstructionService;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Storehouse upgrade: a member files their main inventory into the camp's item chests in one go. The hotbar, armour
 * and offhand stay put. Used from its page, or straight away by right-clicking its piece.
 */
@BPvPListener
@Singleton
public class SortingTable implements Listener {

    public static final String ID = "sorting_table";

    private final CampUpgrades upgrades;
    private final StorehouseChests chests;
    private final ClanManager clanManager;

    @Inject
    public SortingTable(@NotNull CampUpgrades upgrades, @NotNull StorehouseChests chests,
                        @NotNull ClanManager clanManager) {
        this.upgrades = upgrades;
        this.chests = chests;
        this.clanManager = clanManager;
        upgrades.declare(CampStructures.STOREHOUSE, ID, 1);
        upgrades.page(ID, (player, camp, structure, previous) -> {
            if (previous == null) {
                sort(player, camp);
            } else {
                new SortingTableMenu(this, camp, previous).show(player);
            }
        });
    }

    public boolean isActive(@NotNull SiteKey key) {
        return upgrades.has(key, CampStructures.STOREHOUSE, ID);
    }

    /** Files everything in {@code player}'s main inventory into camp {@code key}'s item chests, and tells them how it went. */
    public void sort(@NotNull Player player, @NotNull SiteKey key) {
        final boolean member = clanManager.getClanByPlayer(player)
                .map(clan -> clan.getId() == key.getOwnerId())
                .orElse(false);
        if (!member) {
            tell(player, Translations.component("clans.camp.hall.members_only").color(NamedTextColor.RED));
            return;
        }
        if (!isActive(key)) {
            tell(player, Translations.component("clans.camp.upgrade.sorting_table.inactive").color(NamedTextColor.RED));
            return;
        }
        final Optional<ConstructionService.Worksite> here = chests.here(player, key);
        if (here.isEmpty()) {
            tell(player, Translations.component("clans.camp.storage.not_here").color(NamedTextColor.RED));
            return;
        }

        final World world = here.get().getWorld();
        final List<CampChest> usable = new ArrayList<>();
        final List<Inventory> inventories = new ArrayList<>();
        for (CampChest chest : chests.chests(here.get().getHolding(), world)) {
            chests.inventory(world, chest).ifPresent(inventory -> {
                usable.add(chest);
                inventories.add(inventory);
            });
        }
        if (usable.isEmpty()) {
            tell(player, Translations.component("clans.camp.storage.no_chests").color(NamedTextColor.RED));
            return;
        }

        final PlayerInventory inventory = player.getInventory();
        final List<Integer> slots = new ArrayList<>();
        final List<SortingPlan.Pile<ItemStack>> items = new ArrayList<>();
        for (int slot = 9; slot < 36; slot++) {
            final ItemStack stack = inventory.getItem(slot);
            if (stack != null && !stack.getType().isAir()) {
                slots.add(slot);
                items.add(new SortingPlan.Pile<>(stack, stack.getAmount()));
            }
        }
        if (items.isEmpty()) {
            tell(player, Translations.component("clans.camp.upgrade.sorting_table.nothing").color(NamedTextColor.RED));
            return;
        }

        final List<List<SortingPlan.Pile<ItemStack>>> state = new ArrayList<>();
        for (Inventory chest : inventories) {
            final List<SortingPlan.Pile<ItemStack>> contents = new ArrayList<>();
            for (ItemStack stack : chest.getContents()) {
                contents.add(stack == null || stack.getType().isAir() ? null
                        : new SortingPlan.Pile<>(stack, stack.getAmount()));
            }
            state.add(contents);
        }

        final List<SortingPlan.Move> moves = SortingPlan.plan(state, items, ItemStack::isSimilar,
                ItemStack::getMaxStackSize);
        if (moves.isEmpty()) {
            tell(player, Translations.component("clans.camp.upgrade.sorting_table.full").color(NamedTextColor.RED));
            return;
        }
        final int[] taken = new int[items.size()];
        final Set<Integer> touched = new HashSet<>();
        int moved = 0;
        for (SortingPlan.Move move : moves) {
            final Inventory target = inventories.get(move.getChest());
            final ItemStack there = target.getItem(move.getSlot());
            if (there == null || there.getType().isAir()) {
                final ItemStack placed = items.get(move.getItem()).getKind().clone();
                placed.setAmount(move.getAmount());
                target.setItem(move.getSlot(), placed);
            } else {
                there.setAmount(there.getAmount() + move.getAmount());
                target.setItem(move.getSlot(), there);
            }
            taken[move.getItem()] += move.getAmount();
            touched.add(move.getChest());
            moved += move.getAmount();
        }

        int left = 0;
        for (int i = 0; i < items.size(); i++) {
            final ItemStack stack = inventory.getItem(slots.get(i));
            if (stack == null) {
                continue;
            }
            final int remaining = stack.getAmount() - taken[i];
            left += remaining;
            if (remaining <= 0) {
                inventory.setItem(slots.get(i), null);
            } else {
                stack.setAmount(remaining);
                inventory.setItem(slots.get(i), stack);
            }
        }
        touched.forEach(chest -> chests.save(world, usable.get(chest)));

        tell(player, Translations.component("clans.camp.upgrade.sorting_table.done", Component.text(moved),
                Component.text(touched.size())).color(NamedTextColor.GREEN));
        if (left > 0) {
            tell(player, Translations.component("clans.camp.upgrade.sorting_table.left", Component.text(left))
                    .color(NamedTextColor.YELLOW));
        }
    }

    private static void tell(@NotNull Player player, @NotNull Component message) {
        UtilMessage.message(player, Translations.component("clans.prefix.camp"), message);
    }
}
