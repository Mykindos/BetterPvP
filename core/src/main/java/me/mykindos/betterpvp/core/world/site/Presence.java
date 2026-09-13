package me.mykindos.betterpvp.core.world.site;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Who a player can perceive, and therefore who belongs in any list, message or sound meant for "everyone".
 * <p>
 * Two players perceive each other when they are in the same world. That is the whole rule, and it is enough because
 * an instance holds exactly one world: two parties on separate copies of the same site are in separate worlds, and so
 * is anybody in a world no site owns. Asking here rather than asking the world directly is what makes the rule one
 * decision instead of eighty.
 * <p>
 * Perception is not visibility. A vanished player is still perceived by this and still receives what it is used to
 * deliver, which is why they can read chat while hidden.
 */
@Singleton
public class Presence {

    private final SiteInstances instances;
    private final SiteRegistry registry;

    @Inject
    public Presence(@NotNull SiteInstances instances, @NotNull SiteRegistry registry) {
        this.instances = instances;
        this.registry = registry;
    }

    /** Everyone {@code viewer} can perceive, including themselves. */
    public @NotNull Collection<Player> around(@NotNull Player viewer) {
        return in(viewer.getWorld());
    }

    /** Everyone who can perceive something happening at {@code location}. */
    public @NotNull Collection<Player> at(@NotNull Location location) {
        final World world = location.getWorld();
        return world == null ? List.of() : in(world);
    }

    public @NotNull Collection<Player> in(@NotNull World world) {
        return List.copyOf(world.getPlayers());
    }

    public boolean sees(@NotNull Player viewer, @NotNull Player other) {
        return viewer.getWorld().equals(other.getWorld());
    }

    /** The site whose instance a player is standing in, if any. A world no site owns has no answer. */
    public @NotNull Optional<Site> siteOf(@NotNull Player player) {
        return instances.byWorld(player.getWorld().getName())
                .flatMap(instance -> registry.get(instance.getKey().getSiteId()));
    }

    /** Everyone in any instance of {@code site}, which is more than one group when the site is pooled. */
    public @NotNull List<Player> onSite(@NotNull Site site) {
        final List<Player> present = new ArrayList<>();
        for (SiteInstance instance : instances.all()) {
            if (!instance.getKey().getSiteId().equals(site.getId())) {
                continue;
            }

            final World world = Bukkit.getWorld(instance.getWorldName());
            if (world != null) {
                present.addAll(world.getPlayers());
            }
        }
        return present;
    }

    /** Everyone online who is not standing in any site's world. */
    public @NotNull List<Player> offSite() {
        return Bukkit.getOnlinePlayers().stream()
                .filter(player -> instances.byWorld(player.getWorld().getName()).isEmpty())
                .map(Player.class::cast)
                .toList();
    }
}
