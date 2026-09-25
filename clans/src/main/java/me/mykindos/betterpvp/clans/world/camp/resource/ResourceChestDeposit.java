package me.mykindos.betterpvp.clans.world.camp.resource;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.world.camp.Camp;
import me.mykindos.betterpvp.clans.world.camp.CampConfig;
import me.mykindos.betterpvp.clans.world.camp.CampStore;
import me.mykindos.betterpvp.clans.world.camp.Camps;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemRegistry;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.world.construction.ConstructionService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Right-clicking a resource chest puts every item in the player's inventory that the camp takes into its balance.
 * Items worth nothing stay where they are, and a deposit that would take the camp past what its chests hold is
 * refused whole, with nothing taken. Resources only go in: nothing comes back out of a chest.
 */
@BPvPListener
@Singleton
public class ResourceChestDeposit implements Listener {

    private final ConstructionService construction;
    private final ResourceChests chests;
    private final CampResources resources;
    private final CampStore store;
    private final CampConfig config;
    private final ClanManager clanManager;
    private final ItemFactory itemFactory;
    private final ItemRegistry itemRegistry;

    @Inject
    public ResourceChestDeposit(@NotNull ConstructionService construction, @NotNull ResourceChests chests,
                                @NotNull CampResources resources, @NotNull CampStore store,
                                @NotNull CampConfig config, @NotNull ClanManager clanManager,
                                @NotNull ItemFactory itemFactory, @NotNull ItemRegistry itemRegistry) {
        this.construction = construction;
        this.chests = chests;
        this.resources = resources;
        this.store = store;
        this.config = config;
        this.clanManager = clanManager;
        this.itemFactory = itemFactory;
        this.itemRegistry = itemRegistry;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onChestClick(@NotNull PlayerInteractEvent event) {
        final Block block = event.getClickedBlock();
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND || block == null) {
            return;
        }
        construction.worksite(block.getWorld())
                .filter(worksite -> worksite.getKey().getSiteId().equals(Camps.SITE_ID))
                .filter(worksite -> chests.at(worksite.getHolding(), block.getWorld(), block).isPresent())
                .ifPresent(worksite -> {
                    event.setCancelled(true);
                    store.cached(worksite.getKey().getOwnerId())
                            .ifPresent(camp -> deposit(event.getPlayer(), worksite.getKey().getOwnerId(), camp));
                });
    }

    private void deposit(@NotNull Player player, long clanId, @NotNull Camp camp) {
        final boolean member = clanManager.getClanById(clanId)
                .flatMap(clan -> clan.getMemberByUUID(player.getUniqueId()))
                .isPresent();
        if (!member) {
            message(player, Translations.component("clans.camp.deposit.not_member").color(NamedTextColor.RED));
            return;
        }

        final PlayerInventory inventory = player.getInventory();
        final ItemStack[] contents = inventory.getStorageContents();
        final Map<ResourceKind, Integer> amounts = new EnumMap<>(ResourceKind.class);
        final List<Integer> taken = new ArrayList<>();
        for (int slot = 0; slot < contents.length; slot++) {
            final ItemStack stack = contents[slot];
            if (stack == null || stack.getType().isAir()) {
                continue;
            }
            final Optional<CampConfig.Deposit> value = config.depositValue(itemKey(stack));
            if (value.isPresent()) {
                amounts.merge(value.get().getKind(), value.get().getAmount() * stack.getAmount(), Integer::sum);
                taken.add(slot);
            }
        }
        if (amounts.isEmpty()) {
            message(player, Translations.component("clans.camp.deposit.nothing").color(NamedTextColor.RED));
            return;
        }

        final int capacity = resources.capacity(camp);
        final int held = resources.total(camp);
        final int adding = amounts.values().stream().mapToInt(Integer::intValue).sum();
        if (held + adding > capacity) {
            message(player, Translations.component("clans.camp.deposit.full", Component.text(held),
                    Component.text(capacity)).color(NamedTextColor.RED));
            return;
        }

        taken.forEach(slot -> inventory.setItem(slot, null));
        resources.add(clanId, camp, amounts);
        UtilServer.callEvent(new ResourcesDepositedEvent(Camps.keyFor(clanId), player, amounts));
        message(player, Translations.component("clans.camp.deposit.done", describe(amounts),
                Component.text(held + adding), Component.text(capacity)).color(NamedTextColor.GREEN));
    }

    /** The key a deposit value is listed under: a custom item's own key, or {@code minecraft:<material>}. */
    private @NotNull String itemKey(@NotNull ItemStack stack) {
        return itemFactory.fromItemStack(stack)
                .map(instance -> itemRegistry.getKey(instance.getBaseItem()))
                .map(NamespacedKey::toString)
                .orElseGet(() -> NamespacedKey.minecraft(stack.getType().name().toLowerCase(Locale.ROOT)).toString());
    }

    private static @NotNull Component describe(@NotNull Map<ResourceKind, Integer> amounts) {
        final List<Component> parts = new ArrayList<>();
        amounts.forEach((kind, amount) ->
                parts.add(Translations.component("clans.camp.resource.amount", Component.text(amount), kind.displayName())));
        return Component.join(JoinConfiguration.commas(true), parts);
    }

    private static void message(@NotNull Player player, @NotNull Component message) {
        UtilMessage.plain(player, message);
    }
}
