package me.mykindos.betterpvp.core.world.site;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * Lets a player leave a site of their own accord, sending them to their anchor.
 */
@CustomLog
@Singleton
public class SiteExitService {

    private final SiteInstances instances;
    private final Residency residency;

    @Inject
    public SiteExitService(@NotNull SiteInstances instances, @NotNull Residency residency) {
        this.instances = instances;
        this.residency = residency;
    }

    public @NotNull Optional<SiteInstance> currentInstance(@NotNull Player player) {
        return instances.byWorld(player.getWorld().getName());
    }

    public void leave(@NotNull Player player) {
        final Optional<SiteInstance> instance = currentInstance(player);

        // An instance world with no live instance behind it is exactly where somebody gets stranded: one reaped,
        // released or failed out from under them. That is when they most need this to work, so the way out is offered
        // on the world's name rather than on the registry still knowing about it.
        if (instance.isEmpty() && !isInstanceWorld(player)) {
            UtilMessage.simpleMessage(player, "Travel", Component.text("You are not somewhere you can leave.", NamedTextColor.RED));
            return;
        }

        final Location destination = residency.fallback(player);
        player.teleportAsync(destination).thenAccept(arrived -> {
            if (!Boolean.TRUE.equals(arrived)) {
                log.warn("Player {} failed to teleport off site world {}", player.getName(), player.getWorld().getName()).submit();
            }
        });
    }

    private boolean isInstanceWorld(@NotNull Player player) {
        return player.getWorld().getName().startsWith(SiteWorlds.WORLD_ROOT);
    }
}
