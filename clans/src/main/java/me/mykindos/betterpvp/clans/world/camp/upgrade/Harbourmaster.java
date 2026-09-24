package me.mykindos.betterpvp.clans.world.camp.upgrade;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.world.camp.Camp;
import me.mykindos.betterpvp.clans.world.camp.CampConfig;
import me.mykindos.betterpvp.clans.world.camp.CampPermissions;
import me.mykindos.betterpvp.clans.world.camp.CampStore;
import me.mykindos.betterpvp.clans.world.camp.settler.recruit.CampRecruitment;
import me.mykindos.betterpvp.clans.world.camp.settler.recruit.RecruitConfig;
import me.mykindos.betterpvp.clans.world.camp.structure.CampStructures;
import me.mykindos.betterpvp.clans.world.camp.structure.CampUpgrades;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.world.settler.SettlerAction;
import me.mykindos.betterpvp.core.world.settler.recruit.SettlerCandidate;
import me.mykindos.betterpvp.core.world.settler.recruit.SettlerOdds;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.LongSupplier;

/**
 * Dock upgrade: once per cooldown a camp may either look at who comes on the next boat, which rolls that boat
 * ahead so it brings exactly them, or choose the profession everyone on the next boat has.
 */
@BPvPListener
@Singleton
public class Harbourmaster implements Listener {

    public static final String ID = "harbourmaster";

    private final CampStore store;
    private final CampUpgrades upgrades;
    private final CampConfig config;
    private final CampPermissions permissions;
    private final CampRecruitment recruitment;
    private final RecruitConfig recruitConfig;
    private final LongSupplier clock;

    @Inject
    public Harbourmaster(@NotNull CampStore store, @NotNull CampUpgrades upgrades, @NotNull CampConfig config,
                         @NotNull CampPermissions permissions, @NotNull CampRecruitment recruitment,
                         @NotNull RecruitConfig recruitConfig) {
        this(store, upgrades, config, permissions, recruitment, recruitConfig, System::currentTimeMillis);
    }

    Harbourmaster(@NotNull CampStore store, @NotNull CampUpgrades upgrades, @NotNull CampConfig config,
                  @NotNull CampPermissions permissions, @NotNull CampRecruitment recruitment,
                  @NotNull RecruitConfig recruitConfig, @NotNull LongSupplier clock) {
        this.store = store;
        this.upgrades = upgrades;
        this.config = config;
        this.permissions = permissions;
        this.recruitment = recruitment;
        this.recruitConfig = recruitConfig;
        this.clock = clock;
        upgrades.declare(CampStructures.DOCK, ID, 1);
    }

    /** Whether camp {@code key} has a working Dock with the Harbourmaster. */
    public boolean has(@NotNull SiteKey key) {
        return upgrades.has(key, CampStructures.DOCK, ID);
    }

    /** When the Harbourmaster can next be used, at or before now when it can be used already. */
    public long readyAt(@NotNull SiteKey key) {
        final long hours = config.upgrade(CampStructures.DOCK, ID)
                .map(numbers -> numbers.setting("cooldown-hours", 24))
                .orElse(24);
        return store.cached(key.getOwnerId())
                .map(camp -> camp.getHarbourmasterUsedAt() == 0 ? 0 : camp.getHarbourmasterUsedAt() + hours * 3_600_000L)
                .orElse(0L);
    }

    /** When the next boat is due, or 0 before one is. */
    public long nextBoatAt(@NotNull SiteKey key) {
        return store.cached(key.getOwnerId()).map(Camp::getNextArrivalAt).orElse(0L);
    }

    /** Who comes on the next boat, if the camp looked. */
    public @NotNull List<SettlerCandidate> foreseen(@NotNull SiteKey key) {
        return store.cached(key.getOwnerId()).map(Camp::getNextBoat).orElse(List.of());
    }

    /** The profession everyone on the next boat has, if the camp chose one. */
    public @Nullable String chosen(@NotNull SiteKey key) {
        return store.cached(key.getOwnerId()).map(Camp::getNextBoatProfession).orElse(null);
    }

    /** The professions a camp may choose from: every one a boat can bring. */
    public @NotNull List<String> choices() {
        return recruitConfig.getArrivalOdds().getProfessions().keySet().stream()
                .filter(profession -> !SettlerOdds.NONE.equals(profession))
                .sorted()
                .toList();
    }

    /** Rolls the next boat now so the camp can see who is on it. Returns the reason it was refused, or null. */
    public @Nullable String look(@NotNull Player player, @NotNull SiteKey key) {
        final Camp camp = store.cached(key.getOwnerId()).orElse(null);
        final String refused = refusal(player, key, camp);
        if (refused != null) {
            return refused;
        }
        camp.setNextBoat(recruitment.boat(camp.getNextBoatProfession(), ThreadLocalRandom.current()));
        camp.setHarbourmasterUsedAt(clock.getAsLong());
        store.changed(key.getOwnerId());
        return null;
    }

    /**
     * Makes everyone on the next boat a {@code profession}, replacing a boat the camp looked at. Returns the reason it
     * was refused, or null.
     */
    public @Nullable String choose(@NotNull Player player, @NotNull SiteKey key, @NotNull String profession) {
        final Camp camp = store.cached(key.getOwnerId()).orElse(null);
        final String refused = refusal(player, key, camp);
        if (refused != null) {
            return refused;
        }
        if (!choices().contains(profession)) {
            return "clans.camp.upgrade.harbourmaster.unknown";
        }
        camp.setNextBoatProfession(profession);
        camp.setNextBoat(new ArrayList<>());
        camp.setHarbourmasterUsedAt(clock.getAsLong());
        store.changed(key.getOwnerId());
        return null;
    }

    private @Nullable String refusal(@NotNull Player player, @NotNull SiteKey key, @Nullable Camp camp) {
        if (camp == null) {
            return "core.settler.not_loaded";
        }
        if (!has(key)) {
            return "clans.camp.upgrade.harbourmaster.inactive";
        }
        if (!permissions.allows(player, key.getOwnerId(), SettlerAction.HIRE)) {
            return "clans.settler.card.not_allowed";
        }
        if (clock.getAsLong() < readyAt(key)) {
            return "clans.camp.upgrade.harbourmaster.spent";
        }
        return null;
    }
}
