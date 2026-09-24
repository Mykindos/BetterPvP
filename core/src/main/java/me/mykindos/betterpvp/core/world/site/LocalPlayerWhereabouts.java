package me.mykindos.betterpvp.core.world.site;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** Knows only the players on this server, and leaves out anyone vanished. Call it from the main thread. */
@Singleton
public class LocalPlayerWhereabouts implements PlayerWhereabouts {

    private final Places places;
    private final EffectManager effects;

    @Inject
    public LocalPlayerWhereabouts(@NotNull Places places, @NotNull EffectManager effects) {
        this.places = places;
        this.effects = effects;
    }

    @Override
    public @NotNull CompletableFuture<Map<UUID, Whereabouts>> locate(@NotNull Collection<UUID> players) {
        final Map<UUID, Whereabouts> found = new HashMap<>();
        for (UUID id : players) {
            final Player player = Bukkit.getPlayer(id);
            if (player != null && !effects.hasEffect(player, EffectTypes.VANISH)) {
                found.put(id, places.of(player.getLocation()));
            }
        }
        return CompletableFuture.completedFuture(found);
    }
}
