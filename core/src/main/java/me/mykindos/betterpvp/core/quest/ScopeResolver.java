package me.mykindos.betterpvp.core.quest;

import com.google.inject.Singleton;
import lombok.Setter;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

/**
 * Resolves the participant scope for a quest. Solo and server scopes are handled
 * in core. Clan / party / alliance scopes require leaf modules, which register a
 * resolver (e.g. clans sets {@link #setClanIdResolver}); until then those scopes
 * fall back to solo so quests still function.
 */
@Singleton
public class ScopeResolver {

    /** Maps a player UUID to their clan id, if any. Set by the clans module. */
    @Setter
    private Function<UUID, Optional<String>> clanIdResolver = uuid -> Optional.empty();

    public ScopeKey resolve(Player player, String scopeType) {
        final String uuid = player.getUniqueId().toString();
        return switch (scopeType == null ? "solo" : scopeType) {
            case "server" -> new ScopeKey("server", "global");
            case "clan", "alliance", "party" -> clanIdResolver.apply(player.getUniqueId())
                    .map(id -> new ScopeKey("clan", id))
                    .orElse(new ScopeKey("solo", uuid));
            default -> new ScopeKey("solo", uuid);
        };
    }

    /** Whether this player contributes to progress for the given scope instance. */
    public boolean participates(Player player, String scopeType, String scopeId) {
        return switch (scopeType) {
            case "server" -> true;
            case "clan" -> clanIdResolver.apply(player.getUniqueId()).map(scopeId::equals).orElse(false);
            default -> player.getUniqueId().toString().equals(scopeId);
        };
    }
}
