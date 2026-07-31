package me.mykindos.betterpvp.clans.world.island;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.clans.world.travel.ServerLocation;
import me.mykindos.betterpvp.clans.world.travel.TravelHistory;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.world.WorldHandler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * Lets a player voluntarily leave a discovery island, returning them to their {@link TravelHistory} origin (falling
 * back to {@link WorldHandler#getSpawnLocation()}) and releasing their occupancy slot on arrival.
 */
@CustomLog
@Singleton
public class IslandExitService {

    private final IslandInstanceManager instanceManager;
    private final TravelHistory travelHistory;
    private final WorldHandler worldHandler;

    @Inject
    public IslandExitService(@NotNull IslandInstanceManager instanceManager, @NotNull TravelHistory travelHistory,
                              @NotNull WorldHandler worldHandler) {
        this.instanceManager = instanceManager;
        this.travelHistory = travelHistory;
        this.worldHandler = worldHandler;
    }

    public @NotNull Optional<IslandInstance> currentInstance(@NotNull Player player) {
        return instanceManager.byWorld(player.getWorld().getName());
    }

    public void leave(@NotNull Player player) {
        final Optional<IslandInstance> instanceOptional = currentInstance(player);
        if (instanceOptional.isEmpty()) {
            UtilMessage.simpleMessage(player, "Islands", Component.text("You are not on a discovery island.", NamedTextColor.RED));
            return;
        }

        final IslandInstance instance = instanceOptional.get();
        final Location destination = travelHistory.origin(player)
                .flatMap(ServerLocation::toLocation)
                .orElseGet(worldHandler::getSpawnLocation);

        player.teleportAsync(destination).thenAccept(arrived -> {
            if (Boolean.TRUE.equals(arrived)) {
                instanceManager.exit(instance, player);
            } else {
                log.warn("Player {} failed to teleport off island instance {}", player.getName(), instance.getId()).submit();
            }
        });
    }
}
