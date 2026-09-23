package me.mykindos.betterpvp.clans.world.camp;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.core.components.clans.data.ClanMember;
import me.mykindos.betterpvp.core.world.construction.ConstructionAction;
import me.mykindos.betterpvp.core.world.settler.SettlerAction;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

/**
 * What each rank of a clan may do in its camp. A camp uses the configured defaults until the clan changes something,
 * after which it keeps its own copy.
 */
@Singleton
public class CampPermissions {

    private final ClanManager clanManager;
    private final CampStore store;
    private final CampConfig config;

    @Inject
    public CampPermissions(@NotNull ClanManager clanManager, @NotNull CampStore store, @NotNull CampConfig config) {
        this.clanManager = clanManager;
        this.store = store;
        this.config = config;
    }

    /**
     * Whether {@code player} may take {@code action} in camp {@code clanId}: a member if their rank allows it, a member
     * of an allied clan if the Ally row allows it. The leader may always do everything, so a clan can never lock its
     * own leader out.
     */
    public boolean allows(@NotNull Player player, long clanId, @NotNull ConstructionAction action) {
        final Optional<Clan> owner = clanManager.getClanById(clanId);
        if (owner.isEmpty()) {
            return false;
        }
        final Optional<ClanMember> member = owner.get().getMemberByUUID(player.getUniqueId());
        if (member.isPresent()) {
            return granted(clanId, member.get().getRank()).contains(action);
        }
        return isAlly(owner.get(), player) && allyActions(clanId).contains(action);
    }

    /** Whether {@code player} may take {@code action} on camp {@code clanId}'s settlers. Only members can. */
    public boolean allows(@NotNull Player player, long clanId, @NotNull SettlerAction action) {
        return clanManager.getClanById(clanId)
                .flatMap(owner -> owner.getMemberByUUID(player.getUniqueId()))
                .map(member -> settlerActions(clanId, member.getRank()).contains(action))
                .orElse(false);
    }

    public @NotNull Set<SettlerAction> settlerActions(long clanId, @NotNull ClanMember.MemberRank rank) {
        if (rank == ClanMember.MemberRank.LEADER) {
            return EnumSet.allOf(SettlerAction.class);
        }
        return store.cached(clanId)
                .map(camp -> camp.getSettlerPermissions() == null
                        ? config.defaultSettlerPermissions() : camp.getSettlerPermissions())
                .map(permissions -> permissions.getOrDefault(rank, Set.of()))
                .orElse(Set.of());
    }

    /** Grants or takes away settler {@code action} for {@code rank}. */
    public void set(long clanId, @NotNull ClanMember.MemberRank rank, @NotNull SettlerAction action, boolean allowed) {
        store.cached(clanId).ifPresent(camp -> {
            final Set<SettlerAction> actions = camp.ownSettlerPermissions(config.defaultSettlerPermissions())
                    .computeIfAbsent(rank, unused -> EnumSet.noneOf(SettlerAction.class));
            if (allowed) {
                actions.add(action);
            } else {
                actions.remove(action);
            }
            store.changed(clanId);
        });
    }

    /** Whether {@code player} may open containers in camp {@code clanId}: every member, and allies if allowed. */
    public boolean mayOpenContainers(@NotNull Player player, long clanId) {
        return clanManager.getClanById(clanId)
                .map(owner -> owner.getMemberByUUID(player.getUniqueId()).isPresent()
                        || isAlly(owner, player) && allyContainers(clanId))
                .orElse(false);
    }

    public @NotNull Set<ConstructionAction> allyActions(long clanId) {
        return store.cached(clanId)
                .map(camp -> camp.getAllyActions() == null ? config.defaultAllyActions() : camp.getAllyActions())
                .orElse(Set.of());
    }

    public boolean allyContainers(long clanId) {
        return store.cached(clanId)
                .map(camp -> camp.getAllyContainers() == null ? config.isAllyContainers() : camp.getAllyContainers())
                .orElse(false);
    }

    /** Grants or takes away {@code action} for allies. */
    public void setAlly(long clanId, @NotNull ConstructionAction action, boolean allowed) {
        store.cached(clanId).ifPresent(camp -> {
            final Set<ConstructionAction> actions = camp.ownAllyActions(config.defaultAllyActions());
            if (allowed) {
                actions.add(action);
            } else {
                actions.remove(action);
            }
            store.changed(clanId);
        });
    }

    public void setAllyContainers(long clanId, boolean allowed) {
        store.cached(clanId).ifPresent(camp -> {
            camp.setAllyContainers(allowed);
            store.changed(clanId);
        });
    }

    private static boolean isAlly(@NotNull Clan owner, @NotNull Player player) {
        return owner.getAlliances().stream()
                .anyMatch(alliance -> alliance.getClan().getMembers().stream()
                        .anyMatch(member -> member.getUuid().equals(player.getUniqueId())));
    }

    public @NotNull Set<ConstructionAction> granted(long clanId, @NotNull ClanMember.MemberRank rank) {
        if (rank == ClanMember.MemberRank.LEADER) {
            return EnumSet.allOf(ConstructionAction.class);
        }
        return store.cached(clanId)
                .map(camp -> camp.getPermissions() == null ? config.defaultPermissions() : camp.getPermissions())
                .map(permissions -> permissions.getOrDefault(rank, Set.of()))
                .orElse(Set.of());
    }

    /** Grants or takes away {@code action} for {@code rank}. */
    public void set(long clanId, @NotNull ClanMember.MemberRank rank, @NotNull ConstructionAction action, boolean allowed) {
        store.cached(clanId).ifPresent(camp -> {
            final Set<ConstructionAction> actions = camp.ownPermissions(config.defaultPermissions()).get(rank);
            if (allowed) {
                actions.add(action);
            } else {
                actions.remove(action);
            }
            store.changed(clanId);
        });
    }
}
