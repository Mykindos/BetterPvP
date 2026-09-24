package me.mykindos.betterpvp.clans.world.camp.upgrade;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.world.camp.Camp;
import me.mykindos.betterpvp.clans.world.camp.CampConstruction;
import me.mykindos.betterpvp.clans.world.camp.CampPermissions;
import me.mykindos.betterpvp.clans.world.camp.CampStore;
import me.mykindos.betterpvp.clans.world.camp.structure.CampUpgrades;
import me.mykindos.betterpvp.core.world.settler.SettlerAction;
import me.mykindos.betterpvp.core.world.settler.recruit.SettlerCandidate;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Great Hall upgrade: the clan keeps one hiring board candidate through every new roll of the board until it is hired
 * or let go. The reservation is the candidate's settler id on the camp record.
 */
@Singleton
public class GuestQuarters {

    public static final String ID = "guest_quarters";

    private final CampUpgrades upgrades;
    private final CampStore store;
    private final CampPermissions permissions;

    @Inject
    public GuestQuarters(@NotNull CampUpgrades upgrades, @NotNull CampStore store,
                         @NotNull CampPermissions permissions) {
        this.upgrades = upgrades;
        this.store = store;
        this.permissions = permissions;
        upgrades.declare(CampConstruction.GREAT_HALL, ID, 2);
    }

    public boolean isActive(@NotNull SiteKey key) {
        return upgrades.has(key, CampConstruction.GREAT_HALL, ID);
    }

    /** Whether {@code candidate} is the one camp {@code key} keeps. */
    public boolean isReserved(@NotNull SiteKey key, @NotNull SettlerCandidate candidate) {
        return store.cached(key.getOwnerId())
                .map(Camp::getReservedCandidate)
                .filter(id -> id.equals(candidate.getSettler().getId()))
                .isPresent();
    }

    /**
     * Keeps {@code candidateId} through rerolls in place of any other, or lets it go if it is already kept.
     *
     * @return the translation key of why it was refused, or null once done
     */
    public @Nullable String toggle(@NotNull Player player, @NotNull SiteKey key, @NotNull UUID candidateId) {
        final Camp camp = store.cached(key.getOwnerId()).orElse(null);
        if (camp == null) {
            return "core.settler.not_loaded";
        }
        if (!isActive(key)) {
            return "clans.camp.upgrade.guest_quarters.inactive";
        }
        if (!permissions.allows(player, key.getOwnerId(), SettlerAction.HIRE)) {
            return "clans.settler.card.not_allowed";
        }
        if (camp.getHiringBoard().stream().noneMatch(candidate -> candidate.getSettler().getId().equals(candidateId))) {
            return "clans.settler.recruit.gone";
        }
        camp.setReservedCandidate(candidateId.equals(camp.getReservedCandidate()) ? null : candidateId);
        store.changed(key.getOwnerId());
        return null;
    }

    /**
     * The candidate a new roll of {@code camp}'s board keeps, if the upgrade works and the candidate is still on the
     * board. A reservation that cannot be kept is dropped.
     */
    public @NotNull Optional<SettlerCandidate> carryOver(@NotNull SiteKey key, @NotNull Camp camp) {
        final UUID reserved = camp.getReservedCandidate();
        final Optional<SettlerCandidate> kept = reserved == null || !isActive(key) ? Optional.empty()
                : camp.getHiringBoard().stream()
                .filter(candidate -> Objects.equals(candidate.getSettler().getId(), reserved))
                .findFirst();
        if (kept.isEmpty() && reserved != null) {
            camp.setReservedCandidate(null);
        }
        return kept;
    }
}
