package me.mykindos.betterpvp.clans.world.camp.upgrade;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.world.camp.Camp;
import me.mykindos.betterpvp.clans.world.camp.CampConfig;
import me.mykindos.betterpvp.clans.world.camp.CampStore;
import me.mykindos.betterpvp.clans.world.camp.structure.CampStructures;
import me.mykindos.betterpvp.clans.world.camp.structure.CampUpgrades;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.gamer.properties.GamerProperty;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import me.mykindos.betterpvp.core.world.construction.ConstructionResult;
import me.mykindos.betterpvp.core.world.construction.ConstructionService;
import me.mykindos.betterpvp.core.world.construction.Job;
import me.mykindos.betterpvp.core.world.construction.PlacedStructure;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.Optional;
import me.mykindos.betterpvp.core.utilities.model.tag.CoinsTag;

/**
 * Workshop upgrade: once a day a member can pay coins from their own balance to finish a camp job at once. The price
 * is a rate for every minute the job has left, with a floor.
 */
@Singleton
public class RushOrder {

    public static final String ID = "rush_order";

    private final CampUpgrades upgrades;
    private final CampConfig config;
    private final CampStore store;
    private final ClientManager clientManager;
    private final ClanManager clanManager;
    private final ConstructionService construction;

    @Inject
    public RushOrder(@NotNull CampUpgrades upgrades, @NotNull CampConfig config, @NotNull CampStore store,
                     @NotNull ClientManager clientManager, @NotNull ClanManager clanManager,
                     @NotNull ConstructionService construction) {
        this.upgrades = upgrades;
        this.config = config;
        this.store = store;
        this.clientManager = clientManager;
        this.clanManager = clanManager;
        this.construction = construction;
        upgrades.declare(CampStructures.WORKSHOP, ID, 3);
    }

    public boolean isActive(@NotNull SiteKey key) {
        return upgrades.has(key, CampStructures.WORKSHOP, ID);
    }

    /** What rushing {@code job} costs at {@code now}, in coins. */
    public long price(@NotNull Job job, long now) {
        final CampConfig.UpgradeNumbers numbers = config.upgrade(CampStructures.WORKSHOP, ID).orElse(null);
        final int perMinute = numbers == null ? 20 : numbers.setting("coins-per-minute", 20);
        final int minimum = numbers == null ? 100 : numbers.setting("min-coins", 100);
        return price(remainingMillis(job, now), perMinute, minimum);
    }

    static long price(long remainingMillis, int perMinute, int minimum) {
        final long minutes = (remainingMillis + 59_999) / 60_000;
        return Math.max(minimum, minutes * perMinute);
    }

    /** How long a job has left, at its base pace when its own pace would never finish it. */
    static long remainingMillis(@NotNull Job job, long now) {
        final long remaining = job.remainingMillis(now);
        if (remaining != Long.MAX_VALUE) {
            return remaining;
        }
        return (long) Math.ceil((1.0 - job.progress(now)) * job.getDurationMillis());
    }

    /** How long until camp {@code key} can rush again, or zero if it can now. */
    public long cooldownLeft(@NotNull SiteKey key, long now) {
        return store.cached(key.getOwnerId())
                .map(camp -> cooldownLeft(camp.getRushedAt(), now))
                .orElse(0L);
    }

    static long cooldownLeft(long rushedAt, long now) {
        return rushedAt <= 0 ? 0 : Math.max(0, rushedAt + Duration.ofDays(1).toMillis() - now);
    }

    /** Why {@code player} could not rush {@code structure}'s job in camp {@code key}, or empty if they could. */
    public @NotNull Optional<Component> problem(@NotNull Player player, @NotNull SiteKey key,
                                                @NotNull PlacedStructure structure) {
        final long now = construction.now();
        final Job job = structure.getJob();
        final boolean member = clanManager.getClanByPlayer(player)
                .map(clan -> clan.getId() == key.getOwnerId())
                .orElse(false);
        if (!member) {
            return Optional.of(Translations.component("clans.camp.hall.members_only"));
        }
        if (!isActive(key)) {
            return Optional.of(Translations.component("clans.camp.upgrade.rush_order.inactive"));
        }
        if (job == null || job.isDone(now)) {
            return Optional.of(Translations.component("core.construction.nothing_to_finish"));
        }
        final long left = cooldownLeft(key, now);
        if (left > 0) {
            return Optional.of(Translations.component("clans.camp.upgrade.rush_order.cooldown",
                    Component.text(UtilTime.humanReadableFormat(Duration.ofMillis(Math.max(1000, left))))));
        }
        final long price = price(job, now);
        if (gamer(player).getBalance() < price) {
            return Optional.of(Translations.component("clans.camp.upgrade.rush_order.cannot_afford",
                    CoinsTag.of(Component.text(UtilFormat.formatNumber((int) price), NamedTextColor.YELLOW))));
        }
        return Optional.empty();
    }

    /** Charges {@code player} and finishes the job on {@code structure} in {@code worksite}, if nothing stops it. */
    public @NotNull ConstructionResult rush(@NotNull Player player, @NotNull ConstructionService.Worksite worksite,
                                            @NotNull PlacedStructure structure) {
        final SiteKey key = worksite.getKey();
        final Optional<Component> problem = problem(player, key, structure);
        if (problem.isPresent()) {
            return ConstructionResult.refused(problem.get().color(NamedTextColor.RED));
        }
        final Optional<Camp> camp = store.cached(key.getOwnerId());
        if (camp.isEmpty()) {
            return ConstructionResult.refused("core.construction.no_holding");
        }
        final long now = construction.now();
        final long price = price(structure.getJob(), now);
        final ConstructionResult result = construction.finish(worksite.getWorld(), structure.getId());
        if (result.isSuccess()) {
            final Gamer gamer = gamer(player);
            gamer.saveProperty(GamerProperty.BALANCE, gamer.getBalance() - (int) price);
            camp.get().setRushedAt(now);
            store.changed(key.getOwnerId());
        }
        return result;
    }

    private @NotNull Gamer gamer(@NotNull Player player) {
        return clientManager.search().online(player).getGamer();
    }
}
