package me.mykindos.betterpvp.core.quest.primitive;

import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.quest.model.PrimitiveData;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Registry of condition and action handlers keyed by primitive type id. Core
 * registers the primitives it can evaluate; leaf modules (clans, progression)
 * register theirs (clan_level, profession_level, give_xp, …) via the same
 * registry, so core stays decoupled. One registry, no class-per-primitive.
 */
@Singleton
@CustomLog
public class QuestPrimitiveHandlers {

    private final Map<String, QuestConditionHandler> conditions = new HashMap<>();
    private final Map<String, QuestActionHandler> actions = new HashMap<>();
    private final Map<String, QuestGatingActionHandler> gatingActions = new HashMap<>();
    private final Set<String> warned = new HashSet<>();

    public void registerCondition(String type, QuestConditionHandler handler) {
        conditions.put(type, handler);
    }

    public void registerAction(String type, QuestActionHandler handler) {
        actions.put(type, handler);
    }

    /** Register an action whose outcome resolves later and can fail (see {@link QuestGatingActionHandler}). */
    public void registerGatingAction(String type, QuestGatingActionHandler handler) {
        gatingActions.put(type, handler);
    }

    public boolean isGating(String type) {
        return gatingActions.containsKey(type);
    }

    /** Evaluate a condition. Unknown types fail open (true) with a one-time warning. */
    public boolean evaluate(Player player, PrimitiveData data) {
        QuestConditionHandler handler = conditions.get(data.getType());
        if (handler == null) {
            warnOnce(data.getType());
            return true;
        }
        return handler.test(player, data);
    }

    /**
     * Run an action. Gating actions run fire-and-forget (result discarded).
     * Unknown types are skipped with a one-time warning.
     */
    public void run(Player player, PrimitiveData data) {
        QuestActionHandler handler = actions.get(data.getType());
        if (handler != null) {
            handler.run(player, data);
            return;
        }
        QuestGatingActionHandler gating = gatingActions.get(data.getType());
        if (gating != null) {
            gating.run(player, data);
            return;
        }
        warnOnce(data.getType());
    }

    /** Run a gating action. Unknown types fail open (true) with a one-time warning. */
    public CompletableFuture<Boolean> runGating(Player player, PrimitiveData data) {
        QuestGatingActionHandler handler = gatingActions.get(data.getType());
        if (handler == null) {
            warnOnce(data.getType());
            return CompletableFuture.completedFuture(true);
        }
        return handler.run(player, data);
    }

    /** Run gating actions one after another, short-circuiting on the first failure. */
    public CompletableFuture<Boolean> runGatingSequence(Player player, List<PrimitiveData> gating) {
        CompletableFuture<Boolean> chain = CompletableFuture.completedFuture(true);
        for (PrimitiveData data : gating) {
            chain = chain.thenCompose(ok -> ok ? runGating(player, data) : CompletableFuture.completedFuture(false));
        }
        return chain;
    }

    private void warnOnce(String type) {
        if (warned.add(type)) {
            log.warn("No quest primitive handler registered for '{}' — treating as no-op. " +
                    "A leaf module may need to register it.", type).submit();
        }
    }
}
