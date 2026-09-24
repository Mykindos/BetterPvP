package me.mykindos.betterpvp.clans.world.camp.upgrade;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.world.camp.Camp;
import me.mykindos.betterpvp.clans.world.camp.CampStore;
import me.mykindos.betterpvp.clans.world.camp.Camps;
import me.mykindos.betterpvp.clans.world.camp.structure.CampStructures;
import me.mykindos.betterpvp.clans.world.camp.structure.CampUpgrades;
import me.mykindos.betterpvp.core.components.clans.data.ClanMember;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.world.construction.PlacedStructure;
import me.mykindos.betterpvp.core.world.construction.StructureCondition;
import me.mykindos.betterpvp.core.world.construction.StructureContents;
import me.mykindos.betterpvp.core.world.construction.StructurePieceUseEvent;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Base64;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Storehouse upgrade: a lockbox only the ranks the leader picks can open. Right-clicking the upgrade's piece opens it.
 * Its contents are kept on the camp record by Storehouse, not in the piece's blocks, and are dropped where the
 * Storehouse stood when it is demolished. Allies never open it.
 */
@BPvPListener
@Singleton
public class RankLockbox implements Listener, StructureContents {

    public static final String ID = "rank_lockbox";

    private final CampUpgrades upgrades;
    private final CampStore store;
    private final ClanManager clanManager;
    private final Map<UUID, Inventory> open = new HashMap<>();

    @Inject
    public RankLockbox(@NotNull CampUpgrades upgrades, @NotNull CampStore store, @NotNull ClanManager clanManager) {
        this.upgrades = upgrades;
        this.store = store;
        this.clanManager = clanManager;
        upgrades.declare(CampStructures.STOREHOUSE, ID, 1);
        upgrades.page(ID, (player, camp, structure, previous) -> new RankLockboxMenu(this, player, camp, previous)
                .show(player));
    }

    /** The ranks that may open camp {@code clanId}'s lockboxes. The leader always may. */
    public @NotNull Set<ClanMember.MemberRank> ranks(long clanId) {
        final Set<ClanMember.MemberRank> ranks = EnumSet.of(ClanMember.MemberRank.LEADER);
        ranks.addAll(store.cached(clanId)
                .map(Camp::getLockboxRanks)
                .orElse(defaults()));
        return ranks;
    }

    public boolean mayOpen(@NotNull Player player, long clanId) {
        return rank(player, clanId).map(rank -> ranks(clanId).contains(rank)).orElse(false);
    }

    public boolean isLeader(@NotNull Player player, long clanId) {
        return rank(player, clanId).map(rank -> rank == ClanMember.MemberRank.LEADER).orElse(false);
    }

    /**
     * Lets {@code rank} open camp {@code clanId}'s lockboxes, or stops it. Only the leader changes this, and the
     * leader's own rank cannot be taken off.
     *
     * @return the translation key of why it was refused, or null once done
     */
    public @Nullable String toggle(@NotNull Player player, long clanId, @NotNull ClanMember.MemberRank rank) {
        if (!isLeader(player, clanId)) {
            return "clans.camp.upgrade.rank_lockbox.leader_only";
        }
        final Camp camp = store.cached(clanId).orElse(null);
        if (camp == null) {
            return "core.settler.not_loaded";
        }
        if (rank == ClanMember.MemberRank.LEADER) {
            return null;
        }
        final Set<ClanMember.MemberRank> ranks = EnumSet.noneOf(ClanMember.MemberRank.class);
        ranks.addAll(camp.getLockboxRanks() == null ? defaults() : camp.getLockboxRanks());
        if (!ranks.remove(rank)) {
            ranks.add(rank);
        }
        camp.setLockboxRanks(ranks);
        store.changed(clanId);
        return null;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onUse(@NotNull StructurePieceUseEvent event) {
        final SiteKey site = event.getSite();
        final PlacedStructure structure = event.getStructure();
        if (!site.getSiteId().equals(Camps.SITE_ID) || !ID.equals(event.getUpgrade().getId())
                || !structure.hasUpgrade(ID)) {
            return;
        }
        event.setHandled(true);
        final Player player = event.getPlayer();
        if (structure.getCondition() != StructureCondition.ACTIVE) {
            tell(player, "clans.camp.upgrade.rank_lockbox.inactive");
        } else if (!mayOpen(player, site.getOwnerId())) {
            tell(player, "clans.camp.upgrade.rank_lockbox.denied");
        } else {
            player.openInventory(open.computeIfAbsent(structure.getId(),
                    id -> create(site.getOwnerId(), id)));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onClose(@NotNull InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder(false) instanceof Holder holder)) {
            return;
        }
        save(holder, event.getInventory());
        if (event.getInventory().getViewers().size() <= 1) {
            open.remove(holder.getStructure(), event.getInventory());
        }
    }

    @Override
    public void drop(@NotNull SiteKey site, @NotNull PlacedStructure structure, @NotNull Location at) {
        final Inventory inventory = open.remove(structure.getId());
        if (inventory != null) {
            new ArrayList<>(inventory.getViewers()).forEach(HumanEntity::closeInventory);
        }
        store.cached(site.getOwnerId()).ifPresent(camp -> {
            final List<String> items = camp.getLockboxes().remove(structure.getId());
            if (items == null || at.getWorld() == null) {
                return;
            }
            items.stream().map(RankLockbox::decode).filter(Objects::nonNull)
                    .forEach(item -> at.getWorld().dropItemNaturally(at, item));
            store.changed(site.getOwnerId());
        });
    }

    private @NotNull Inventory create(long clanId, @NotNull UUID structure) {
        final Holder holder = new Holder(clanId, structure);
        final Inventory inventory = Bukkit.createInventory(holder, 27,
                Translations.component("clans.camp.upgrade.rank_lockbox.name"));
        holder.setInventory(inventory);
        final List<String> stored = store.cached(clanId)
                .map(camp -> camp.getLockboxes().get(structure))
                .orElse(null);
        if (stored != null) {
            for (int slot = 0; slot < stored.size() && slot < inventory.getSize(); slot++) {
                inventory.setItem(slot, decode(stored.get(slot)));
            }
        }
        return inventory;
    }

    private void save(@NotNull Holder holder, @NotNull Inventory inventory) {
        store.cached(holder.getClanId()).ifPresent(camp -> {
            final List<String> encoded = new ArrayList<>(inventory.getSize());
            boolean any = false;
            for (ItemStack item : inventory.getContents()) {
                final String value = item == null || item.getType().isAir() ? null
                        : Base64.getEncoder().encodeToString(item.serializeAsBytes());
                any |= value != null;
                encoded.add(value);
            }
            if (any) {
                camp.getLockboxes().put(holder.getStructure(), encoded);
            } else {
                camp.getLockboxes().remove(holder.getStructure());
            }
            store.changed(holder.getClanId());
        });
    }

    private @NotNull Optional<ClanMember.MemberRank> rank(@NotNull Player player, long clanId) {
        return clanManager.getClanById(clanId)
                .flatMap(clan -> clan.getMemberByUUID(player.getUniqueId()))
                .map(ClanMember::getRank);
    }

    private static @NotNull Set<ClanMember.MemberRank> defaults() {
        return EnumSet.of(ClanMember.MemberRank.LEADER, ClanMember.MemberRank.ADMIN);
    }

    private static @Nullable ItemStack decode(@Nullable String value) {
        return value == null ? null : ItemStack.deserializeBytes(Base64.getDecoder().decode(value));
    }

    private static void tell(@NotNull Player player, @NotNull String key) {
        UtilMessage.message(player, Translations.component("clans.prefix.camp"),
                Translations.component(key).color(NamedTextColor.RED));
    }

    /** Marks a lockbox's inventory with the camp and Storehouse it belongs to. */
    @Getter
    private static final class Holder implements InventoryHolder {
        private final long clanId;
        private final UUID structure;
        @Setter
        private Inventory inventory;

        private Holder(long clanId, @NotNull UUID structure) {
            this.clanId = clanId;
            this.structure = structure;
        }

        @Override
        public @NotNull Inventory getInventory() {
            return inventory;
        }
    }
}
