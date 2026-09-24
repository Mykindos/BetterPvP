package me.mykindos.betterpvp.clans.world.camp.upgrade;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.world.camp.structure.CampStructures;
import me.mykindos.betterpvp.clans.world.camp.structure.CampUpgrades;
import me.mykindos.betterpvp.core.client.properties.ClientProperty;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.components.clans.data.ClanMember;
import me.mykindos.betterpvp.core.inventory.item.Item;
import me.mykindos.betterpvp.core.inventory.item.impl.SimpleItem;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import me.mykindos.betterpvp.core.world.construction.PlacedStructure;
import me.mykindos.betterpvp.core.world.site.PlayerWhereabouts;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import me.mykindos.betterpvp.core.world.site.Whereabouts;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Barracks upgrade: a roll of the clan's members, those online first with where they are on the network, then the
 * rest with when they were last seen. Where they are comes from {@link PlayerWhereabouts}.
 */
@CustomLog
@BPvPListener
@Singleton
public class MusterRoll implements Listener {

    public static final String ID = "muster_roll";

    private final Clans clans;
    private final ClanManager clanManager;
    private final ClientManager clientManager;
    private final PlayerWhereabouts whereabouts;

    @Inject
    public MusterRoll(@NotNull Clans clans, @NotNull ClanManager clanManager, @NotNull ClientManager clientManager,
                      @NotNull PlayerWhereabouts whereabouts, @NotNull CampUpgrades upgrades) {
        this.clans = clans;
        this.clanManager = clanManager;
        this.clientManager = clientManager;
        this.whereabouts = whereabouts;
        upgrades.declare(CampStructures.BARRACKS, ID, 2);
        upgrades.page(ID, this::open);
    }

    private void open(@NotNull Player player, @NotNull SiteKey camp, @NotNull PlacedStructure structure,
                      @Nullable Windowed previous) {
        final Clan clan = clanManager.getClanById(camp.getOwnerId()).orElse(null);
        if (clan == null) {
            return;
        }
        final List<ClanMember> members = List.copyOf(clan.getMembers());
        whereabouts.locate(members.stream().map(ClanMember::getUuid).toList())
                .thenCompose(online -> lastSeen(members, online)
                        .thenAccept(seen -> Bukkit.getScheduler().runTask(clans, () -> {
                            if (!player.isOnline()) {
                                return;
                            }
                            final long now = System.currentTimeMillis();
                            final List<Item> items = order(members, online, seen).stream()
                                    .map(member -> (Item) new SimpleItem(item(member, online.get(member.getUuid()),
                                            seen.get(member.getUuid()), now)))
                                    .toList();
                            new MemberListMenu(Translations.component("clans.camp.upgrade.muster_roll.name"), items,
                                    previous).show(player);
                        })))
                .exceptionally(error -> {
                    log.error("Could not read the muster roll of clan {}", camp.getOwnerId(), error).submit();
                    return null;
                });
    }

    /** When each member not in {@code online} last left, for those it is known of. */
    private @NotNull CompletableFuture<Map<UUID, Long>> lastSeen(@NotNull Collection<ClanMember> members,
                                                                @NotNull Map<UUID, Whereabouts> online) {
        final Map<UUID, Long> seen = new ConcurrentHashMap<>();
        final CompletableFuture<?>[] reads = members.stream()
                .map(ClanMember::getUuid)
                .filter(id -> !online.containsKey(id))
                .map(id -> clientManager.search().offline(id).thenAccept(client -> client
                        .flatMap(found -> found.<Long>getProperty(ClientProperty.LAST_LOGIN))
                        .filter(time -> time > 0)
                        .ifPresent(time -> seen.put(id, time))))
                .toArray(CompletableFuture[]::new);
        return CompletableFuture.allOf(reads).thenApply(ignored -> seen);
    }

    /** The order the roll lists members in: online by name, then offline by most recently seen, then by name. */
    static @NotNull List<ClanMember> order(@NotNull Collection<ClanMember> members,
                                           @NotNull Map<UUID, Whereabouts> online,
                                           @NotNull Map<UUID, Long> lastSeen) {
        return members.stream()
                .sorted(Comparator.<ClanMember, Boolean>comparing(member -> !online.containsKey(member.getUuid()))
                        .thenComparing(Comparator.<ClanMember>comparingLong(member ->
                                lastSeen.getOrDefault(member.getUuid(), Long.MIN_VALUE)).reversed())
                        .thenComparing(MemberListMenu::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private static @NotNull ItemView item(@NotNull ClanMember member, @Nullable Whereabouts where,
                                          @Nullable Long seen, long now) {
        final ItemView.ItemViewBuilder view = ItemView.builder()
                .material(where != null ? Material.PLAYER_HEAD : Material.SKELETON_SKULL)
                .displayName(Component.text(MemberListMenu.name(member),
                        where != null ? NamedTextColor.GREEN : NamedTextColor.GRAY).decorate(TextDecoration.BOLD))
                .frameLore(true);
        if (where != null) {
            return view
                    .lore(MemberListMenu.place(where).color(NamedTextColor.WHITE))
                    .lore(Translations.component("clans.camp.upgrade.muster_roll.server",
                            Component.text(where.getServer(), NamedTextColor.YELLOW)).color(NamedTextColor.GRAY))
                    .build();
        }
        return view
                .lore(Translations.component("clans.camp.upgrade.muster_roll.offline").color(NamedTextColor.RED))
                .lore((seen == null
                        ? Translations.component("clans.camp.upgrade.muster_roll.never")
                        : Translations.component("clans.camp.upgrade.muster_roll.seen", MemberListMenu.ago(now - seen)))
                        .color(NamedTextColor.GRAY))
                .build();
    }
}
