package me.mykindos.betterpvp.clans.world.camp.settler.recruit;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.world.camp.Camp;
import me.mykindos.betterpvp.clans.world.camp.CampPermissions;
import me.mykindos.betterpvp.clans.world.camp.CampStore;
import me.mykindos.betterpvp.clans.world.camp.Camps;
import me.mykindos.betterpvp.clans.world.camp.settler.CampTraits;
import me.mykindos.betterpvp.clans.world.camp.settler.CampWideTraits;
import me.mykindos.betterpvp.clans.world.camp.settler.SettlerConfig;
import me.mykindos.betterpvp.clans.world.camp.structure.CampStructures;
import me.mykindos.betterpvp.clans.world.camp.upgrade.GuestQuarters;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.world.construction.ConstructionService;
import me.mykindos.betterpvp.core.world.settler.Settler;
import me.mykindos.betterpvp.core.world.settler.SettlerAction;
import me.mykindos.betterpvp.core.world.settler.SettlerGenerator;
import me.mykindos.betterpvp.core.world.settler.SettlerRarity;
import me.mykindos.betterpvp.core.world.settler.SettlerResult;
import me.mykindos.betterpvp.core.world.settler.SettlerService;
import me.mykindos.betterpvp.core.world.settler.SettlerTemplate;
import me.mykindos.betterpvp.core.world.settler.TraitGroup;
import me.mykindos.betterpvp.core.world.settler.TraitRegistry;
import me.mykindos.betterpvp.core.world.settler.recruit.SettlerCandidate;
import me.mykindos.betterpvp.core.world.settler.recruit.SettlerOdds;
import me.mykindos.betterpvp.core.world.site.SiteInstance;
import me.mykindos.betterpvp.core.world.site.SiteInstances;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.LongSupplier;
import java.util.stream.Stream;
import me.mykindos.betterpvp.core.utilities.model.tag.CoinsTag;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * How settlers come to a camp. Boats bring candidates to the Dock while it works, and they wait there a while. The
 * Steward keeps a hiring board that turns over on its own or for a fee. Clans reaching a milestone level are sent a
 * settler who waits at the Dock until there is room. Recruiters bring boats sooner and Hagglers bring prices down. A
 * candidate kept by {@link GuestQuarters} stays on the board when it turns over.
 * <p>
 * Everything is worked out from timestamps each minute, on the server holding the camp's world, so boats that came
 * while the camp was closed are still waiting if their time is not up.
 */
@BPvPListener
@Singleton
public class CampRecruitment implements Listener {

    /** At most this many boats are caught up at once, so a camp closed for weeks does not roll hundreds. */
    private static final int MAX_BOATS = 6;

    private final CampStore store;
    private final SettlerService settlers;
    private final SettlerGenerator generator;
    private final SettlerConfig settlerConfig;
    private final RecruitConfig config;
    private final TraitRegistry traits;
    private final ConstructionService construction;
    private final SiteInstances instances;
    private final ClanManager clanManager;
    private final CampPermissions permissions;
    private final CampCoins coins;
    private final CampWideTraits campWide;
    private final GuestQuarters guestQuarters;
    private final LongSupplier clock;

    @Inject
    public CampRecruitment(@NotNull CampStore store, @NotNull SettlerService settlers,
                           @NotNull SettlerGenerator generator, @NotNull SettlerConfig settlerConfig,
                           @NotNull RecruitConfig config, @NotNull TraitRegistry traits,
                           @NotNull ConstructionService construction, @NotNull SiteInstances instances,
                           @NotNull ClanManager clanManager, @NotNull CampPermissions permissions,
                           @NotNull CampCoins coins, @NotNull CampWideTraits campWide,
                           @NotNull GuestQuarters guestQuarters) {
        this(store, settlers, generator, settlerConfig, config, traits, construction, instances, clanManager,
                permissions, coins, campWide, guestQuarters, System::currentTimeMillis);
    }

    CampRecruitment(@NotNull CampStore store, @NotNull SettlerService settlers, @NotNull SettlerGenerator generator,
                    @NotNull SettlerConfig settlerConfig, @NotNull RecruitConfig config,
                    @NotNull TraitRegistry traits, @NotNull ConstructionService construction,
                    @NotNull SiteInstances instances, @NotNull ClanManager clanManager,
                    @NotNull CampPermissions permissions, @NotNull CampCoins coins,
                    @NotNull CampWideTraits campWide, @NotNull GuestQuarters guestQuarters,
                    @NotNull LongSupplier clock) {
        this.store = store;
        this.settlers = settlers;
        this.generator = generator;
        this.settlerConfig = settlerConfig;
        this.config = config;
        this.traits = traits;
        this.construction = construction;
        this.instances = instances;
        this.clanManager = clanManager;
        this.permissions = permissions;
        this.coins = coins;
        this.campWide = campWide;
        this.guestQuarters = guestQuarters;
        this.clock = clock;
    }

    @UpdateEvent(delay = 60_000)
    public void tick() {
        for (SiteInstance instance : new ArrayList<>(instances.all())) {
            final World world = Bukkit.getWorld(instance.getWorldName());
            if (world != null && instance.getKey().getSiteId().equals(Camps.SITE_ID)) {
                settle(instance.getKey(), dockWorking(world));
            }
        }
    }

    /** Sends away candidates who stopped waiting, brings in boats that are due and sends milestone settlers. */
    void settle(@NotNull SiteKey key, boolean dockWorking) {
        final Camp camp = store.cached(key.getOwnerId()).orElse(null);
        if (camp == null) {
            return;
        }
        final long now = clock.getAsLong();
        final Random random = ThreadLocalRandom.current();
        camp.getArrivals().removeIf(candidate -> candidate.isExpired(now));

        final long level = clanManager.getClanById(key.getOwnerId()).map(Clan::getLevel).orElse(0L);
        boolean sent = false;
        for (Map.Entry<Integer, RecruitConfig.Milestone> milestone : config.getMilestones().entrySet()) {
            if (milestone.getKey() <= level && camp.getMilestones().add(milestone.getKey())) {
                sent = true;
                final String profession = milestone.getValue().getProfession();
                camp.getArrivals().add(new SettlerCandidate(roll(milestone.getValue().getRarity(),
                        RecruitConfig.ANY.equals(profession) ? config.getArrivalOdds().profession(random)
                                : SettlerOdds.NONE.equals(profession) ? null : profession,
                        "milestone", random), 0, 0));
            }
        }
        if (sent) {
            UtilServer.callEvent(new SettlerBoatEvent(key, true));
        }

        final long interval = interval(key);
        if (camp.getNextArrivalAt() == 0 || !dockWorking) {
            if (camp.getNextArrivalAt() == 0 || now >= camp.getNextArrivalAt()) {
                camp.setNextArrivalAt(now + interval);
            }
        } else {
            int boats = 0;
            while (now >= camp.getNextArrivalAt() && boats++ < MAX_BOATS) {
                final long leaves = camp.getNextArrivalAt() + config.getArrivalWait().toMillis();
                if (now < leaves) {
                    camp.getArrivals().addAll(land(camp, random, leaves));
                    UtilServer.callEvent(new SettlerBoatEvent(key, false));
                }
                camp.setNextArrivalAt(camp.getNextArrivalAt() + interval);
            }
            if (now >= camp.getNextArrivalAt()) {
                camp.setNextArrivalAt(now + interval);
            }
        }
        store.changed(key.getOwnerId());
    }

    /** The hiring board, rolled again once it is old enough. */
    public @NotNull List<SettlerCandidate> board(@NotNull SiteKey key) {
        final Camp camp = store.cached(key.getOwnerId()).orElse(null);
        if (camp == null) {
            return List.of();
        }
        final long now = clock.getAsLong();
        if (camp.getBoardRolledAt() == 0 || now - camp.getBoardRolledAt() >= config.getBoardRefresh().toMillis()) {
            rollBoard(key, camp, now);
        }
        return camp.getHiringBoard();
    }

    /** When the board next turns over by itself. */
    public long boardRefreshAt(@NotNull SiteKey key) {
        return store.cached(key.getOwnerId())
                .map(camp -> camp.getBoardRolledAt() + config.getBoardRefresh().toMillis())
                .orElse(0L);
    }

    public @NotNull List<SettlerCandidate> arrivals(@NotNull SiteKey key) {
        return store.cached(key.getOwnerId()).map(Camp::getArrivals).orElse(List.of());
    }

    public @NotNull Optional<SettlerCandidate> find(@NotNull SiteKey key, @NotNull UUID id) {
        return store.cached(key.getOwnerId()).flatMap(camp -> find(camp, id));
    }

    /** What {@code candidate} costs this camp now, after any Haggler. */
    public long price(@NotNull SiteKey key, @NotNull SettlerCandidate candidate) {
        return Math.round(candidate.getPrice() * (1 - best(key, CampTraits.HAGGLER, "discount", 0.10)));
    }

    /** Takes a candidate on, paid for by {@code player}. Refused, and nothing is paid, if there is no room. */
    public @NotNull SettlerResult hire(@NotNull Player player, @NotNull SiteKey key, @NotNull UUID id) {
        final Camp camp = store.cached(key.getOwnerId()).orElse(null);
        final SettlerCandidate candidate = camp == null ? null : find(camp, id).orElse(null);
        if (candidate == null || candidate.isExpired(clock.getAsLong())) {
            return SettlerResult.refused("clans.settler.recruit.gone");
        }
        if (!permissions.allows(player, key.getOwnerId(), SettlerAction.HIRE)) {
            return SettlerResult.refused("clans.settler.card.not_allowed");
        }
        if (settlers.roster(key).map(roster -> roster.size() >= settlers.populationCap(key)).orElse(true)) {
            return SettlerResult.refused("core.settler.population_full", Component.text(settlers.populationCap(key)));
        }
        final long price = price(key, candidate);
        if (!coins.take(player, price)) {
            return SettlerResult.refused("clans.settler.recruit.cannot_afford",
                    CoinsTag.of(Component.text(price, NamedTextColor.YELLOW)));
        }

        final SettlerResult result = settlers.grant(key, candidate.getSettler());
        if (!result.isSuccess()) {
            coins.give(player, price);
            return result;
        }
        camp.getArrivals().remove(candidate);
        camp.getHiringBoard().remove(candidate);
        store.changed(key.getOwnerId());
        UtilServer.callEvent(new SettlerHiredEvent(key, player, candidate.getSettler(), price));
        return result;
    }

    /** Sends a candidate away without taking them on. */
    public @NotNull SettlerResult turnAway(@NotNull Player player, @NotNull SiteKey key, @NotNull UUID id) {
        final Camp camp = store.cached(key.getOwnerId()).orElse(null);
        final SettlerCandidate candidate = camp == null ? null : find(camp, id).orElse(null);
        if (candidate == null) {
            return SettlerResult.refused("clans.settler.recruit.gone");
        }
        if (!permissions.allows(player, key.getOwnerId(), SettlerAction.HIRE)) {
            return SettlerResult.refused("clans.settler.card.not_allowed");
        }
        camp.getArrivals().remove(candidate);
        camp.getHiringBoard().remove(candidate);
        store.changed(key.getOwnerId());
        return SettlerResult.done(candidate.getSettler());
    }

    /** Rolls a new hiring board now, paid for by {@code player}. */
    public @Nullable String reroll(@NotNull Player player, @NotNull SiteKey key) {
        final Camp camp = store.cached(key.getOwnerId()).orElse(null);
        if (camp == null) {
            return "core.settler.not_loaded";
        }
        if (!permissions.allows(player, key.getOwnerId(), SettlerAction.HIRE)) {
            return "clans.settler.card.not_allowed";
        }
        if (!coins.take(player, config.getReroll())) {
            return "clans.settler.recruit.cannot_afford_reroll";
        }
        rollBoard(key, camp, clock.getAsLong());
        return null;
    }

    private void rollBoard(@NotNull SiteKey key, @NotNull Camp camp, long now) {
        final Random random = ThreadLocalRandom.current();
        final List<SettlerCandidate> board = new ArrayList<>(guestQuarters.carryOver(key, camp).stream().toList());
        for (int i = board.size(); i < config.getBoardSize(); i++) {
            final SettlerRarity rarity = config.getBoardOdds().rarity(random);
            board.add(new SettlerCandidate(roll(rarity, config.getBoardOdds().profession(random), "hiring", random),
                    config.getBoardPrices().getOrDefault(rarity, 0L), 0));
        }
        camp.setHiringBoard(board);
        camp.setBoardRolledAt(now);
        store.changed(key.getOwnerId());
    }

    /** The boat landing now: the one rolled ahead for the camp if there is one, else a new one. */
    private @NotNull List<SettlerCandidate> land(@NotNull Camp camp, @NotNull Random random, long leaves) {
        final List<SettlerCandidate> boat = camp.getNextBoat().isEmpty()
                ? boat(camp.getNextBoatProfession(), random)
                : new ArrayList<>(camp.getNextBoat());
        camp.setNextBoat(new ArrayList<>());
        camp.setNextBoatProfession(null);
        boat.forEach(candidate -> candidate.setExpiresAt(leaves));
        return boat;
    }

    /**
     * Rolls a boat's candidates without landing it, each waiting with no time limit until it lands.
     *
     * @param profession the profession every candidate has, or null to roll each from the arrival odds
     */
    public @NotNull List<SettlerCandidate> boat(@Nullable String profession, @NotNull Random random) {
        final Integer count = SettlerOdds.pick(config.getArrivalCounts(), random);
        final List<SettlerCandidate> boat = new ArrayList<>();
        for (int i = 0; i < (count == null ? 1 : count); i++) {
            final SettlerRarity rarity = config.getArrivalOdds().rarity(random);
            final Settler settler = roll(rarity, profession != null ? profession
                    : config.getArrivalOdds().profession(random), "dock", random);
            final long price = rarity == SettlerRarity.COMMON
                    ? (campWide(settler) ? config.getCampWideTraitPrice() : 0)
                    : config.getArrivalPrices().getOrDefault(rarity, 0L);
            boat.add(new SettlerCandidate(settler, price, 0));
        }
        return boat;
    }

    private @NotNull Settler roll(@NotNull SettlerRarity rarity, @Nullable String profession, @NotNull String source,
                                  @NotNull Random random) {
        return generator.roll(SettlerTemplate.builder().rarity(rarity).profession(profession).source(source).build(),
                settlerConfig.getTable(), random);
    }

    private boolean campWide(@NotNull Settler settler) {
        return settler.getTraits().stream()
                .flatMap(id -> traits.find(id).stream())
                .anyMatch(trait -> trait.getGroup() == TraitGroup.SITE_WIDE);
    }

    /** How long between boats, sooner with a Recruiter in the camp. */
    long interval(@NotNull SiteKey key) {
        return (long) (config.getArrivalEvery().toMillis() * (1 - best(key, CampTraits.RECRUITER, "sooner", 0.15)));
    }

    /** The strongest effect any settler in the camp has from {@code trait}, as a share, never above 90%. */
    private double best(@NotNull SiteKey key, @NotNull String trait, @NotNull String number, double fallback) {
        return settlers.roster(key)
                .map(roster -> Math.min(0.9, campWide.best(roster, trait, number, fallback)))
                .orElse(0.0);
    }

    private boolean dockWorking(@NotNull World world) {
        final long now = construction.now();
        return construction.worksite(world)
                .map(worksite -> worksite.getHolding().ofType(CampStructures.DOCK).stream()
                        .anyMatch(dock -> dock.status(now).isUsable()))
                .orElse(false);
    }

    private static @NotNull Optional<SettlerCandidate> find(@NotNull Camp camp, @NotNull UUID id) {
        return Stream.concat(camp.getArrivals().stream(), camp.getHiringBoard().stream())
                .filter(candidate -> candidate.getSettler().getId().equals(id))
                .findFirst();
    }
}
