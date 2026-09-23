package me.mykindos.betterpvp.clans.world.camp.protection;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.brauw.mapper.region.CuboidRegion;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.world.camp.Camps;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.world.content.WorldContent;
import me.mykindos.betterpvp.core.world.mapper.RegionIndex;
import me.mykindos.betterpvp.core.world.zone.CompositeBounds;
import me.mykindos.betterpvp.core.world.zone.GlobalBounds;
import me.mykindos.betterpvp.core.world.zone.RegionBounds;
import me.mykindos.betterpvp.core.world.zone.Zone;
import me.mykindos.betterpvp.core.world.zone.ZoneBounds;
import me.mykindos.betterpvp.core.world.zone.ZoneGameMode;
import me.mykindos.betterpvp.core.world.zone.ZoneRuleContainer;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Covers a whole camp world with one zone that keeps its land as it is, played in adventure, and each {@code farm}
 * cuboid with one where members play in survival to plant and harvest. Both sit just above the world's baseline, so a
 * resource node or the dock on top of them keeps its own rules.
 */
@Singleton
public class CampGrounds implements WorldContent {

    /** Carried by every zone inside a camp. */
    public static final String TAG = "camp";
    public static final String FARM = "farm";

    private final Camps camps;
    private final ClanManager clanManager;
    private final ClientManager clientManager;

    @Inject
    public CampGrounds(@NotNull Camps camps, @NotNull ClanManager clanManager, @NotNull ClientManager clientManager) {
        this.camps = camps;
        this.clanManager = clanManager;
        this.clientManager = clientManager;
    }

    @Override
    public @NotNull List<Zone> zones(@NotNull World world, @NotNull RegionIndex regions) {
        final List<Zone> zones = new ArrayList<>();
        final String id = world.getName().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_.-]", "_");

        zones.add(Zone.builder()
                .key(Key.key("clans", "camp_" + id))
                .displayName(name(world))
                .tag(TAG)
                .bounds(GlobalBounds.world(world))
                .priority(1)
                .gameMode(ZoneGameMode.of(GameMode.ADVENTURE))
                .rules(new ZoneRuleContainer().add(new CampGroundsRule(camps, clientManager, false)))
                .build());

        final List<ZoneBounds> farms = regions.find(FARM, CuboidRegion.class).stream()
                .map(farm -> (ZoneBounds) RegionBounds.of(farm))
                .toList();
        if (!farms.isEmpty()) {
            zones.add(Zone.builder()
                    .key(Key.key("clans", "camp_farm_" + id))
                    .displayName(Translations.component("clans.camp.zone.farm"))
                    .tag(TAG)
                    .tag(FARM)
                    .bounds(CompositeBounds.of(farms))
                    .priority(2)
                    .gameMode(player -> camps.isMember(player, world) ? GameMode.SURVIVAL : null)
                    .rules(new ZoneRuleContainer().add(new CampGroundsRule(camps, clientManager, true)))
                    .build());
        }

        return zones;
    }

    private @NotNull Component name(@NotNull World world) {
        return camps.clanOf(world).stream()
                .mapToObj(clanManager::getClanById)
                .flatMap(clan -> clan.stream())
                .map(Clan::getName)
                .findFirst()
                .map(clan -> Translations.component("clans.camp.zone.name", Component.text(clan)))
                .orElseGet(() -> Translations.component("clans.camp.zone.unnamed"));
    }
}
