package me.mykindos.betterpvp.clans.world.sailing;

import me.mykindos.betterpvp.clans.clans.zone.ClanZones;
import me.mykindos.betterpvp.clans.world.SceneSpawn;
import me.mykindos.betterpvp.clans.world.WorldContent;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.world.mapper.RegionIndex;
import me.mykindos.betterpvp.core.world.zone.GlobalBounds;
import me.mykindos.betterpvp.core.world.zone.NoBuildRule;
import me.mykindos.betterpvp.core.world.zone.Zone;
import me.mykindos.betterpvp.core.world.zone.ZoneRuleContainer;
import me.mykindos.betterpvp.core.world.zone.Zones;
import net.kyori.adventure.text.Component;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * The rules of the open sea: look, do not touch.
 * <p>
 * A crossing is a few minutes stood on someone else's ship in a world that is deleted when they arrive. Anything built
 * or broken out here is thrown away with the instance, so the only thing free building achieves is letting somebody
 * dismantle the vessel their crew is standing on.
 * <p>
 * Covers the whole world rather than the hull, because everything outside the hull is ocean nobody should be editing
 * either.
 */
public class OceanContent implements WorldContent {

    private final ClientManager clientManager;

    public OceanContent(@NotNull ClientManager clientManager) {
        this.clientManager = clientManager;
    }

    @Override
    public @NotNull List<Zone> zones(@NotNull World world, @NotNull RegionIndex regions) {
        final ZoneRuleContainer rules = new ZoneRuleContainer();
        rules.add(new NoBuildRule(clientManager));

        return List.of(Zone.builder()
                .key(ClanZones.regionKey("open-sea"))
                .displayName(Component.text("The Open Sea"))
                .bounds(GlobalBounds.world(world))
                .priority(ClanZones.SERVER_REGION_PRIORITY)
                .tag(Zones.NO_BUILD)
                .rules(rules)
                .build());
    }

    @Override
    public @NotNull List<SceneSpawn> sceneObjects(@NotNull World world, @NotNull RegionIndex regions) {
        return List.of();
    }
}
