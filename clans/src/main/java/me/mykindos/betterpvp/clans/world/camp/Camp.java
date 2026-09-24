package me.mykindos.betterpvp.clans.world.camp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;
import me.mykindos.betterpvp.clans.world.camp.upgrade.DeputyPost;
import me.mykindos.betterpvp.clans.world.camp.upgrade.LedgerEntry;
import me.mykindos.betterpvp.clans.world.camp.upgrade.QueuedAction;
import me.mykindos.betterpvp.core.components.clans.data.ClanMember;
import me.mykindos.betterpvp.core.world.construction.ConstructionAction;
import me.mykindos.betterpvp.core.world.construction.Holding;
import me.mykindos.betterpvp.core.world.settler.Roster;
import me.mykindos.betterpvp.core.world.settler.SettlerAction;
import me.mykindos.betterpvp.core.world.settler.recruit.SettlerCandidate;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * A camp as it is kept between visits: everything about it that its world does not hold by itself.
 * <p>
 * The world is built from a template and can be rebuilt from one at any time, so nothing that has to survive that
 * lives in its blocks. What does live here is what the clan chose and what it has raised.
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Camp {

    /** Which template the world is built from, or null while the clan has not chosen and takes the default. */
    private @Nullable String skin;

    /** The structures the clan has raised or is raising. */
    private Holding holding = new Holding();

    /** The settlers living in the camp. */
    private Roster roster = new Roster();

    /** Coins set aside to pay the camp's settlers. */
    private long wageFund;

    /** Candidates waiting at the Dock, from boats and milestones. */
    private List<SettlerCandidate> arrivals = new ArrayList<>();

    /** When the next boat comes in, or 0 before the first is due. */
    private long nextArrivalAt;

    /** Candidates rolled ahead for the next boat, who come on it instead of new ones. Empty when none are. */
    private List<SettlerCandidate> nextBoat = new ArrayList<>();

    /** The profession everyone on the next boat has, or null for the arrival odds. */
    private @Nullable String nextBoatProfession;

    /** When the Harbourmaster was last used, or 0 before it ever was. */
    private long harbourmasterUsedAt;

    /** Candidates on the Steward's hiring board. */
    private List<SettlerCandidate> hiringBoard = new ArrayList<>();

    /** When the hiring board was last rolled, or 0 before it ever was. */
    private long boardRolledAt;

    /** The hiring board candidate kept through rerolls, or null for none. */
    private @Nullable UUID reservedCandidate;

    /** Members who pay what the wage fund cannot. */
    private Set<UUID> wageContributors = new HashSet<>();

    /** Coins each member has paid toward wages on {@link #wageContributionDay}. */
    private Map<UUID, Long> wageContributions = new HashMap<>();

    /** The day {@link #wageContributions} counts, in days since the epoch. */
    private long wageContributionDay;

    /** The clan levels whose settlers have been sent. */
    private Set<Integer> milestones = new HashSet<>();

    /** Whether the camp has been given the settlers every camp starts with. */
    private boolean startingSettlers;

    /** Resource balance, by resource id. */
    private Map<String, Integer> resources = new LinkedHashMap<>();

    /** What each rank may do, or null for the configured defaults. */
    private @Nullable Map<ClanMember.MemberRank, Set<ConstructionAction>> permissions;

    /** What members of allied clans may do, or null for the configured defaults. */
    private @Nullable Set<ConstructionAction> allyActions;

    /** Whether members of allied clans may open containers, or null for the configured default. */
    private @Nullable Boolean allyContainers;

    /** What each rank may do to the camp's settlers, or null for the configured defaults. */
    private @Nullable Map<ClanMember.MemberRank, Set<SettlerAction>> settlerPermissions;

    /** The action waiting to start when the camp's next job is claimed, or null when nothing is queued. */
    private @Nullable QueuedAction queuedAction;

    /** The last queued action that could not start, kept so members hear about it when they arrive. */
    private @Nullable QueuedAction droppedAction;

    /** When a job was last rushed, or 0 before one ever was. */
    private long rushedAt;

    /** Members who respawn at the Barracks' second door. */
    private Set<UUID> secondDoor = new HashSet<>();

    /** What members did in the camp, oldest first. Only the newest entries are kept. */
    private List<LedgerEntry> ledger = new ArrayList<>();

    /** Where the Deputy Steward stands, or null while it is not placed. */
    private @Nullable DeputyPost deputy;

    /** When the feast laid at the Feast table ends, or 0 before one ever was. */
    private long feastUntil;

    /** When the Great bell was last rung, or 0 before it ever was. */
    private long bellRungAt;

    /** The ranks that may open a Storehouse's rank lockbox, or null for the defaults. The leader always may. */
    private @Nullable Set<ClanMember.MemberRank> lockboxRanks;

    /** What each Storehouse's rank lockbox holds, by structure id, as encoded items by slot. */
    private Map<UUID, List<String>> lockboxes = new HashMap<>();

    public int getResource(String resource) {
        return resources.getOrDefault(resource, 0);
    }

    /** The permissions this camp has set, copying them from {@code defaults} the first time any are changed. */
    public Map<ClanMember.MemberRank, Set<ConstructionAction>> ownPermissions(
            Map<ClanMember.MemberRank, Set<ConstructionAction>> defaults) {
        if (permissions == null) {
            permissions = new EnumMap<>(ClanMember.MemberRank.class);
            defaults.forEach((rank, actions) -> {
                final Set<ConstructionAction> copy = EnumSet.noneOf(ConstructionAction.class);
                copy.addAll(actions);
                permissions.put(rank, copy);
            });
        }
        return permissions;
    }

    /** The settler permissions this camp has set, copying them from {@code defaults} the first time any change. */
    public Map<ClanMember.MemberRank, Set<SettlerAction>> ownSettlerPermissions(
            Map<ClanMember.MemberRank, Set<SettlerAction>> defaults) {
        if (settlerPermissions == null) {
            settlerPermissions = new EnumMap<>(ClanMember.MemberRank.class);
            defaults.forEach((rank, actions) -> {
                final Set<SettlerAction> copy = EnumSet.noneOf(SettlerAction.class);
                copy.addAll(actions);
                settlerPermissions.put(rank, copy);
            });
        }
        return settlerPermissions;
    }

    /** The actions this camp lets allies take, copying them from {@code defaults} the first time they are changed. */
    public Set<ConstructionAction> ownAllyActions(Set<ConstructionAction> defaults) {
        if (allyActions == null) {
            allyActions = EnumSet.noneOf(ConstructionAction.class);
            allyActions.addAll(defaults);
        }
        return allyActions;
    }
}
