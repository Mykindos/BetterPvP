package me.mykindos.betterpvp.clans.world.generic;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.world.site.SiteWorlds;
import me.mykindos.betterpvp.core.framework.events.ServerStartEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.world.model.BPvPWorld;
import me.mykindos.betterpvp.core.world.zone.GlobalBounds;
import me.mykindos.betterpvp.core.world.zone.Zone;
import me.mykindos.betterpvp.core.world.zone.ZoneGameMode;
import me.mykindos.betterpvp.core.world.zone.ZoneManager;
import me.mykindos.betterpvp.core.world.zone.Zones;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The ground a world stands on, for worlds that are played in something other than the default.
 * <p>
 * Two of them exist: the clans world, whose unclaimed land is the {@link Zones#WILDERNESS wilderness} players build and
 * fight in, and every island instance, which is somewhere a player is sent to strip. Both cover their whole world at
 * priority zero, under everything else, so a claim, a safe region or a mine on top of them wins outright and the
 * baseline only answers where nothing else does. Everywhere with no baseline at all - the spawn world, a hand-authored
 * island, a boss arena - has no opinion, and adventure is what players get.
 * <p>
 * Registered straight off world load rather than through the world-content pipeline: content is built from a world's
 * Mapper data and a world with no data-points contributes nothing, which is a fine outcome for a dock and a very bad
 * one for whether the server's main world can be built in.
 */
@BPvPListener
@Singleton
public class WorldBaselineZones implements Listener {

    private final ZoneManager zoneManager;
    private final Map<String, Zone> registered = new ConcurrentHashMap<>();

    @Inject
    public WorldBaselineZones(@NotNull ZoneManager zoneManager) {
        this.zoneManager = zoneManager;
    }

    @EventHandler
    public void onServerStart(ServerStartEvent event) {
        Bukkit.getWorlds().forEach(this::register);
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        register(event.getWorld());
    }

    @EventHandler
    public void onWorldUnload(WorldUnloadEvent event) {
        final Zone zone = registered.remove(event.getWorld().getName());
        if (zone != null) {
            zoneManager.unregister(zone);
        }
    }

    private void register(@NotNull World world) {
        final Zone zone = baselineFor(world);
        if (zone == null) {
            return;
        }

        // A world can be handed to us twice (the start-up sweep and a load event); registering the same zone twice
        // would leave a stale copy in the ambient set.
        final Zone previous = registered.put(world.getName(), zone);
        if (previous != null) {
            zoneManager.unregister(previous);
        }
        zoneManager.register(zone);
    }

    private @Nullable Zone baselineFor(@NotNull World world) {
        if (world.getName().equals(BPvPWorld.MAIN_WORLD_NAME)) {
            return zone(world, "wilderness", Translations.component("clans.territory.wilderness"), true);
        }
        if (world.getName().startsWith(SiteWorlds.WORLD_ROOT)) {
            // Keyed by world: every live instance registers its own, and two of them must not collide.
            final String key = ("island_" + world.getName()).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_.-]", "_");
            return zone(world, key, Component.text("Island"), false);
        }
        return null;
    }

    private @NotNull Zone zone(@NotNull World world, @NotNull String key, @NotNull Component displayName,
                               boolean wilderness) {
        final Zone.ZoneBuilder builder = Zone.builder()
                .key(Key.key("clans", key))
                .displayName(displayName)
                .bounds(GlobalBounds.world(world))
                .priority(0)
                .gameMode(ZoneGameMode.of(GameMode.SURVIVAL));
        if (wilderness) {
            // What tells the rest of the plugin this is open land rather than an owned area, so claiming and the
            // territory display still treat it as untouched ground.
            builder.tag(Zones.WILDERNESS);
        }
        return builder.build();
    }
}
