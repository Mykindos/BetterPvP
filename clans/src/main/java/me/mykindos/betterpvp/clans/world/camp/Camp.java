package me.mykindos.betterpvp.clans.world.camp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;
import me.mykindos.betterpvp.core.components.clans.data.ClanMember;
import me.mykindos.betterpvp.core.world.construction.ConstructionAction;
import me.mykindos.betterpvp.core.world.construction.Holding;
import me.mykindos.betterpvp.core.world.settler.Roster;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

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

    /** Resource balance, by resource id. */
    private Map<String, Integer> resources = new LinkedHashMap<>();

    /** What each rank may do, or null for the configured defaults. */
    private @Nullable Map<ClanMember.MemberRank, Set<ConstructionAction>> permissions;

    /** What members of allied clans may do, or null for the configured defaults. */
    private @Nullable Set<ConstructionAction> allyActions;

    /** Whether members of allied clans may open containers, or null for the configured default. */
    private @Nullable Boolean allyContainers;

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

    /** The actions this camp lets allies take, copying them from {@code defaults} the first time they are changed. */
    public Set<ConstructionAction> ownAllyActions(Set<ConstructionAction> defaults) {
        if (allyActions == null) {
            allyActions = EnumSet.noneOf(ConstructionAction.class);
            allyActions.addAll(defaults);
        }
        return allyActions;
    }
}
