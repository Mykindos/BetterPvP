package me.mykindos.betterpvp.clans.world.camp.upgrade;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.world.camp.Camp;
import me.mykindos.betterpvp.clans.world.camp.CampStore;
import me.mykindos.betterpvp.clans.world.camp.Camps;
import me.mykindos.betterpvp.clans.world.camp.structure.CampStructures;
import me.mykindos.betterpvp.clans.world.camp.structure.CampUpgrades;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.world.construction.ConstructionAction;
import me.mykindos.betterpvp.core.world.construction.ConstructionResult;
import me.mykindos.betterpvp.core.world.construction.ConstructionService;
import me.mykindos.betterpvp.core.world.construction.Holding;
import me.mykindos.betterpvp.core.world.construction.PlacedStructure;
import me.mykindos.betterpvp.core.world.construction.StructureCatalogue;
import me.mykindos.betterpvp.core.world.construction.StructureClaimedEvent;
import me.mykindos.betterpvp.core.world.construction.StructureCondition;
import me.mykindos.betterpvp.core.world.construction.StructureType;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;
import me.mykindos.betterpvp.core.utilities.model.ChatIcon;

/**
 * Workshop upgrade: while a job runs, the camp can queue one advance or repair on an idle structure. It starts when
 * the camp's next job is claimed, as the member who queued it, and is paid for then. One that cannot start is dropped,
 * and members hear about it straight away or when they next arrive at the camp.
 */
@BPvPListener
@Singleton
public class BuildQueue implements Listener {

    public static final String ID = "build_queue";

    private final Clans clans;
    private final CampUpgrades upgrades;
    private final CampStore store;
    private final Camps camps;
    private final ClanManager clanManager;
    private final ConstructionService construction;
    private final StructureCatalogue catalogue;

    @Inject
    public BuildQueue(@NotNull Clans clans, @NotNull CampUpgrades upgrades, @NotNull CampStore store,
                      @NotNull Camps camps, @NotNull ClanManager clanManager,
                      @NotNull ConstructionService construction, @NotNull StructureCatalogue catalogue) {
        this.clans = clans;
        this.upgrades = upgrades;
        this.store = store;
        this.camps = camps;
        this.clanManager = clanManager;
        this.construction = construction;
        this.catalogue = catalogue;
        upgrades.declare(CampStructures.WORKSHOP, ID, 2);
    }

    public boolean isActive(@NotNull SiteKey key) {
        return upgrades.has(key, CampStructures.WORKSHOP, ID);
    }

    /** What camp {@code key} has queued, if anything. */
    public @NotNull Optional<QueuedAction> queued(@NotNull SiteKey key) {
        return store.cached(key.getOwnerId()).map(Camp::getQueuedAction);
    }

    /** What would be queued for {@code structure}: an advance while it works, a repair while it is broken. */
    public static @NotNull Optional<ConstructionAction> queueable(@NotNull PlacedStructure structure,
                                                                 @NotNull StructureType type) {
        if (structure.getJob() != null) {
            return Optional.empty();
        }
        final StructureCondition condition = structure.getCondition();
        if (condition == StructureCondition.ACTIVE && type.hasStage(structure.getStage() + 1)) {
            return Optional.of(ConstructionAction.ADVANCE);
        }
        if (condition == StructureCondition.DISABLED || condition == StructureCondition.NEEDS_REPAIR) {
            return Optional.of(ConstructionAction.REPAIR);
        }
        return Optional.empty();
    }

    /** Whether any structure but {@code id} has a job, running or waiting to be claimed. */
    public static boolean busyElsewhere(@NotNull Holding holding, @NotNull UUID id) {
        return holding.getStructures().stream()
                .anyMatch(structure -> !structure.getId().equals(id) && structure.getJob() != null);
    }

    /** Queues {@code action} on {@code structure} for {@code player}, if the camp has room for it. */
    public boolean queue(@NotNull Player player, @NotNull SiteKey key, @NotNull PlacedStructure structure,
                         @NotNull ConstructionAction action) {
        final Optional<Camp> camp = store.cached(key.getOwnerId());
        if (camp.isEmpty() || camp.get().getQueuedAction() != null) {
            return false;
        }
        camp.get().setQueuedAction(new QueuedAction(structure.getId(), structure.getType(), action,
                player.getUniqueId()));
        store.changed(key.getOwnerId());
        return true;
    }

    /** Removes and returns what camp {@code key} has queued. */
    public @Nullable QueuedAction clear(@NotNull SiteKey key) {
        final Camp camp = store.cached(key.getOwnerId()).orElse(null);
        if (camp == null || camp.getQueuedAction() == null) {
            return null;
        }
        final QueuedAction queued = camp.getQueuedAction();
        camp.setQueuedAction(null);
        store.changed(key.getOwnerId());
        return queued;
    }

    /** What a queued action is called, such as "Workshop advance". */
    public @NotNull Component describe(@NotNull QueuedAction queued) {
        final Component name = catalogue.find(queued.getType())
                .map(StructureType::getDisplayName)
                .orElseGet(() -> Component.text(queued.getType()));
        return Translations.component("clans.camp.upgrade.build_queue.entry",
                Translations.component("clans.camp.upgrade.build_queue.action."
                        + queued.getAction().name().toLowerCase(Locale.ROOT)), name);
    }

    @EventHandler
    public void onClaim(@NotNull StructureClaimedEvent event) {
        final SiteKey key = event.getSite();
        if (!key.getSiteId().equals(Camps.SITE_ID) || !isActive(key)) {
            return;
        }
        final QueuedAction queued = clear(key);
        if (queued != null) {
            final World world = event.getWorld();
            UtilServer.runTask(clans, () -> start(key, world, queued));
        }
    }

    private void start(@NotNull SiteKey key, @NotNull World world, @NotNull QueuedAction queued) {
        final Player player = Bukkit.getPlayer(queued.getQueuedBy());
        final boolean advance = queued.getAction() == ConstructionAction.ADVANCE;
        final ConstructionResult result;
        if (player != null) {
            result = advance ? construction.advance(player, world, queued.getStructure())
                    : construction.repair(player, world, queued.getStructure());
        } else {
            result = advance ? construction.advance(world, queued.getStructure())
                    : construction.repair(world, queued.getStructure());
        }

        if (result.isSuccess()) {
            tellOnline(key, ChatIcon.NEWS.line(Translations.component("clans.camp.upgrade.build_queue.started",
                    describe(queued).color(NamedTextColor.WHITE)).color(NamedTextColor.YELLOW)), null);
            return;
        }
        queued.setDroppedAt(construction.now());
        Component message = Translations.component("clans.camp.upgrade.build_queue.dropped",
                        describe(queued).color(NamedTextColor.YELLOW))
                .color(NamedTextColor.RED);
        if (result.getReason() != null) {
            message = message.appendNewline()
                    .append(Translations.component("clans.camp.reason", result.getReason()).color(NamedTextColor.GRAY));
        }
        tellOnline(key, ChatIcon.PROBLEM.line(message), queued);
        store.cached(key.getOwnerId()).ifPresent(camp -> camp.setDroppedAction(queued));
        store.changed(key.getOwnerId());
    }

    /** Tells every online member of the camp's clan, noting them in {@code dropped} as told if given. */
    private void tellOnline(@NotNull SiteKey key, @NotNull Component message, @Nullable QueuedAction dropped) {
        clanManager.getClanById(key.getOwnerId()).ifPresent(clan -> {
            for (Player member : clan.getMembersAsPlayers()) {
                UtilMessage.plain(member, message);
                if (dropped != null) {
                    dropped.getTold().add(member.getUniqueId());
                }
            }
        });
    }

    @EventHandler
    public void onChangedWorld(@NotNull PlayerChangedWorldEvent event) {
        arrive(event.getPlayer());
    }

    @EventHandler
    public void onJoin(@NotNull PlayerJoinEvent event) {
        arrive(event.getPlayer());
    }

    private void arrive(@NotNull Player player) {
        UtilServer.runTaskLater(clans, () -> {
            if (!player.isOnline() || !camps.isMember(player, player.getWorld())) {
                return;
            }
            final OptionalLong clan = camps.clanOf(player.getWorld());
            if (clan.isEmpty()) {
                return;
            }
            store.cached(clan.getAsLong()).ifPresent(camp -> {
                final QueuedAction dropped = camp.getDroppedAction();
                if (dropped == null || dropped.getTold().contains(player.getUniqueId())
                        || construction.now() - dropped.getDroppedAt() > Duration.ofDays(1).toMillis()) {
                    return;
                }
                dropped.getTold().add(player.getUniqueId());
                store.changed(clan.getAsLong());
                UtilMessage.plain(player, ChatIcon.PROBLEM.line(
                        Translations.component("clans.camp.upgrade.build_queue.dropped_notice",
                                describe(dropped).color(NamedTextColor.YELLOW))
                                .color(NamedTextColor.RED)));
            });
        }, 40L);
    }
}
