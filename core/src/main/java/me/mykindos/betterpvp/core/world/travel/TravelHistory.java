package me.mykindos.betterpvp.core.world.travel;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.gamer.properties.GamerProperty;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * Remembers where a player was standing right before their most recent voyage, backed by
 * {@link GamerProperty#TRAVEL_ORIGIN}. The origin is a {@link ServerLocation} so a traveller who is later moved to a
 * different backend server still has a meaningful (if unreachable-from-here) record of where they came from.
 */
@Singleton
public class TravelHistory {

    private final ClientManager clientManager;

    @Inject
    public TravelHistory(ClientManager clientManager) {
        this.clientManager = clientManager;
    }

    public void recordOrigin(@NotNull Player player) {
        final Gamer gamer = clientManager.search().online(player).getGamer();
        gamer.saveProperty(GamerProperty.TRAVEL_ORIGIN, ServerLocation.local(player.getLocation()).serialize());
    }

    public @NotNull Optional<ServerLocation> origin(@NotNull Player player) {
        final Gamer gamer = clientManager.search().online(player).getGamer();
        final Optional<String> stored = gamer.getProperty(GamerProperty.TRAVEL_ORIGIN);
        if (stored.isEmpty() || stored.get().isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(ServerLocation.parse(stored.get()));
    }
}
