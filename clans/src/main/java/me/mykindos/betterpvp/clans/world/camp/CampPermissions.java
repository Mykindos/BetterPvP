package me.mykindos.betterpvp.clans.world.camp;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.core.components.clans.data.ClanMember;
import me.mykindos.betterpvp.core.world.construction.ConstructionAction;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
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
     * Whether {@code player} is a member of the clan that owns camp {@code clanId} and their rank may take
     * {@code action}. The leader may always do everything, so a clan can never lock its own leader out.
     */
    public boolean allows(@NotNull Player player, long clanId, @NotNull ConstructionAction action) {
        return clanManager.getClanById(clanId)
                .flatMap(clan -> clan.getMemberByUUID(player.getUniqueId()))
                .map(member -> granted(clanId, member.getRank()).contains(action))
                .orElse(false);
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
