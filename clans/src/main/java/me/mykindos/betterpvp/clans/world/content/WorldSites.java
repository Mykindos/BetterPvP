package me.mykindos.betterpvp.clans.world.content;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.world.site.SiteRegistry;
import me.mykindos.betterpvp.core.world.site.SiteWorlds;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

/**
 * Selects the worlds a site runs in, so content can be authored against a place rather than a world name. A site that
 * adopts a world matches that one world, and one that is cloned or owned matches every copy.
 */
@Singleton
public class WorldSites {

    private final SiteRegistry registry;

    @Inject
    public WorldSites(@NotNull SiteRegistry registry) {
        this.registry = registry;
    }

    public @NotNull WorldSelector selector(@NotNull String siteId) {
        return world -> matches(siteId, world);
    }

    private boolean matches(@NotNull String siteId, @NotNull World world) {
        return registry.get(siteId).map(site -> switch (site.getWorldSource().getKind()) {
            case ADOPT -> world.getName().equals(site.getWorldSource().getValue());
            case CLONE -> world.getName().startsWith(SiteWorlds.worldNamePrefix(site.getId()));
            case OWN -> world.getName().startsWith(site.getWorldSource().getValue());
        }).orElse(false);
    }
}
