package me.mykindos.betterpvp.core.quest.primitive;

import me.mykindos.betterpvp.core.quest.model.PrimitiveData;
import org.bukkit.entity.Player;

import java.util.concurrent.CompletableFuture;

/**
 * An action primitive whose outcome resolves later and can fail — e.g. a
 * conversation that must run to its end, not merely start. A quest stage whose
 * actions include a gating action only completes once every gate resolves true.
 * <p>
 * The handler owns the lifecycle of its future: it must guarantee resolution
 * (e.g. on player quit), since callers hold no state of their own to clean up.
 */
@FunctionalInterface
public interface QuestGatingActionHandler {
    CompletableFuture<Boolean> run(Player player, PrimitiveData data);
}
