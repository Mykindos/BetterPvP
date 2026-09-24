package me.mykindos.betterpvp.clans.world.camp.upgrade;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.world.camp.structure.CampStructures;
import me.mykindos.betterpvp.clans.world.camp.structure.CampUpgrades;
import me.mykindos.betterpvp.core.combat.damagelog.DamageLog;
import me.mykindos.betterpvp.core.combat.damagelog.DamageLogManager;
import me.mykindos.betterpvp.core.combat.death.events.CustomDeathEvent;
import me.mykindos.betterpvp.core.components.clans.data.ClanMember;
import me.mykindos.betterpvp.core.inventory.item.Item;
import me.mykindos.betterpvp.core.inventory.item.impl.SimpleItem;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import me.mykindos.betterpvp.core.world.construction.PlacedStructure;
import me.mykindos.betterpvp.core.world.site.Places;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Barracks upgrade: a board listing where each member last died and to whom. Every clan member's death on this server
 * is recorded in the {@link CasualtyStore}, fitted or not, so the board is complete from the day it goes up.
 */
@CustomLog
@BPvPListener
@Singleton
public class CasualtyBoard implements Listener {

    public static final String ID = "casualty_board";

    private final Clans clans;
    private final ClanManager clanManager;
    private final CasualtyStore casualties;
    private final Places places;
    private final DamageLogManager damageLogs;

    @Inject
    public CasualtyBoard(@NotNull Clans clans, @NotNull ClanManager clanManager, @NotNull CampUpgrades upgrades,
                         @NotNull CasualtyStore casualties, @NotNull Places places,
                         @NotNull DamageLogManager damageLogs) {
        this.clans = clans;
        this.clanManager = clanManager;
        this.casualties = casualties;
        this.places = places;
        this.damageLogs = damageLogs;
        upgrades.declare(CampStructures.BARRACKS, ID, 1);
        upgrades.page(ID, this::open);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(@NotNull CustomDeathEvent event) {
        if (!(event.getKilled() instanceof Player player) || clanManager.getClanByPlayer(player).isEmpty()) {
            return;
        }
        casualties.save(new Casualty(player.getUniqueId(), places.of(player.getLocation()), killer(event),
                System.currentTimeMillis()));
    }

    /** Who or what killed them: the killer's name, else the reason or damage cause, else null. */
    private @Nullable Component killer(@NotNull CustomDeathEvent event) {
        final LivingEntity killer = event.getKiller();
        if (killer instanceof Player player) {
            return Component.text(player.getName());
        }
        if (killer != null) {
            final Component custom = killer.customName();
            return custom != null ? custom : Component.translatable(killer.getType().translationKey());
        }
        final String[] reason = event.getReason();
        if (reason != null && reason.length > 0) {
            return Component.text(String.join(", ", reason));
        }
        return damageLogs.getObject(event.getKilled().getUniqueId())
                .filter(entries -> !entries.isEmpty())
                .map(ConcurrentLinkedDeque::getLast)
                .map(DamageLog::getDamageCause)
                .map(cause -> (Component) Component.text(cause.getDisplayName()))
                .orElse(null);
    }

    private void open(@NotNull Player player, @NotNull SiteKey camp, @NotNull PlacedStructure structure,
                      @Nullable Windowed previous) {
        final Clan clan = clanManager.getClanById(camp.getOwnerId()).orElse(null);
        if (clan == null) {
            return;
        }
        final List<ClanMember> members = List.copyOf(clan.getMembers());
        casualties.lastDeaths(members.stream().map(ClanMember::getUuid).toList())
                .thenAccept(deaths -> Bukkit.getScheduler().runTask(clans, () -> {
                    if (!player.isOnline()) {
                        return;
                    }
                    final long now = System.currentTimeMillis();
                    final List<Item> items = order(members, deaths).stream()
                            .map(member -> (Item) new SimpleItem(item(member, deaths.get(member.getUuid()), now)))
                            .toList();
                    new MemberListMenu(Translations.component("clans.camp.upgrade.casualty_board.name"), items,
                            previous).show(player);
                }))
                .exceptionally(error -> {
                    log.error("Could not read the casualty board of clan {}", camp.getOwnerId(), error).submit();
                    return null;
                });
    }

    /** The order the board lists members in: the newest death first, then members with none, by name. */
    static @NotNull List<ClanMember> order(@NotNull Collection<ClanMember> members,
                                           @NotNull Map<UUID, Casualty> deaths) {
        return members.stream()
                .sorted(Comparator.<ClanMember>comparingLong(member -> {
                            final Casualty death = deaths.get(member.getUuid());
                            return death == null ? Long.MIN_VALUE : death.getDiedAt();
                        }).reversed()
                        .thenComparing(MemberListMenu::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private static @NotNull ItemView item(@NotNull ClanMember member, @Nullable Casualty death, long now) {
        final ItemView.ItemViewBuilder view = ItemView.builder()
                .material(death == null ? Material.PLAYER_HEAD : Material.SKELETON_SKULL)
                .displayName(Component.text(MemberListMenu.name(member),
                        death == null ? NamedTextColor.GRAY : NamedTextColor.YELLOW).decorate(TextDecoration.BOLD))
                .frameLore(true);
        if (death == null) {
            return view.lore(Translations.component("clans.camp.upgrade.casualty_board.none")
                    .color(NamedTextColor.GRAY)).build();
        }
        final Component killer = death.getKiller();
        return view
                .lore(MemberListMenu.place(death.getWhere()).color(NamedTextColor.WHITE))
                .lore((killer == null
                        ? Translations.component("clans.camp.upgrade.casualty_board.unknown")
                        : Translations.component("clans.camp.upgrade.casualty_board.killer",
                        killer.color(NamedTextColor.RED)))
                        .color(NamedTextColor.GRAY))
                .lore(MemberListMenu.ago(now - death.getDiedAt()).color(NamedTextColor.GRAY))
                .build();
    }
}
